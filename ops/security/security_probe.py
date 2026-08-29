#!/usr/bin/env python3
"""travel 项目非破坏性安全探测脚本。

默认仅允许 localhost/127.0.0.1/::1，执行只读请求或预期被拒绝的写请求。
不会注册账号、上传文件、删除数据、调用真实 AI 生成接口或进行高并发压测。
"""
from __future__ import annotations

import argparse
import concurrent.futures
import http.client
import json
import socket
import ssl
import sys
import time
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, Iterable, Optional
from urllib.parse import urljoin, urlparse

LOCAL_HOSTS = {"127.0.0.1", "localhost", "::1"}
SENSITIVE_HEADERS = {
    "server",
    "x-powered-by",
    "x-application-context",
}
SECURITY_HEADERS = {
    "x-content-type-options": "nosniff",
    "x-frame-options": None,
    "content-security-policy": None,
    "referrer-policy": None,
    "permissions-policy": None,
}


@dataclass
class Finding:
    test: str
    severity: str
    passed: bool
    method: str
    path: str
    status: Optional[int]
    evidence: str
    recommendation: str
    elapsed_ms: Optional[int] = None


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="travel 本地非破坏性安全探测")
    parser.add_argument("--base-url", default="http://127.0.0.1:8090", help="Gateway 地址")
    parser.add_argument("--timeout", type=float, default=4.0, help="单请求超时秒数")
    parser.add_argument("--output", default="ops/security/security-report.json", help="JSON 报告路径")
    parser.add_argument("--allow-private-target", action="store_true", help="允许 RFC1918 私网目标；仍拒绝公网")
    parser.add_argument("--check-direct-services", action="store_true", help="探测 8091-8095 是否可被直连")
    parser.add_argument("--check-rate-limit", action="store_true", help="执行最多 12 个并发 GET 检查限流")
    return parser.parse_args()


def is_private_ip(host: str) -> bool:
    import ipaddress
    try:
        return ipaddress.ip_address(host).is_private
    except ValueError:
        try:
            addresses = {item[4][0] for item in socket.getaddrinfo(host, None)}
            return bool(addresses) and all(ipaddress.ip_address(ip).is_private for ip in addresses)
        except (socket.gaierror, ValueError):
            return False


def validate_target(base_url: str, allow_private: bool) -> None:
    parsed = urlparse(base_url)
    if parsed.scheme not in {"http", "https"} or not parsed.hostname:
        raise SystemExit("base-url 必须是有效的 http/https URL")
    host = parsed.hostname.lower()
    if host in LOCAL_HOSTS:
        return
    if allow_private and is_private_ip(host):
        return
    raise SystemExit(
        f"安全保护：拒绝探测非本机目标 {host}。仅允许 localhost；"
        "授权私网环境可显式传 --allow-private-target。"
    )


def request(base_url: str, method: str, path: str, timeout: float,
            headers: Optional[Dict[str, str]] = None,
            body: Optional[str] = None) -> tuple[Optional[int], Dict[str, str], str, int]:
    url = urljoin(base_url.rstrip("/") + "/", path.lstrip("/"))
    parsed = urlparse(url)
    conn_cls = http.client.HTTPSConnection if parsed.scheme == "https" else http.client.HTTPConnection
    kwargs = {"timeout": timeout}
    if parsed.scheme == "https":
        kwargs["context"] = ssl.create_default_context()
    conn = conn_cls(parsed.hostname, parsed.port, **kwargs)
    target = parsed.path or "/"
    if parsed.query:
        target += "?" + parsed.query
    req_headers = {"User-Agent": "travel-local-security-probe/1.0", "Accept": "application/json"}
    if headers:
        req_headers.update(headers)
    encoded = body.encode("utf-8") if body is not None else None
    started = time.perf_counter()
    try:
        conn.request(method, target, body=encoded, headers=req_headers)
        response = conn.getresponse()
        payload = response.read(8192).decode("utf-8", errors="replace")
        elapsed = int((time.perf_counter() - started) * 1000)
        return response.status, {k.lower(): v for k, v in response.getheaders()}, payload, elapsed
    except (OSError, http.client.HTTPException) as exc:
        elapsed = int((time.perf_counter() - started) * 1000)
        return None, {}, f"{type(exc).__name__}: {exc}", elapsed
    finally:
        conn.close()


def status_is_denied(status: Optional[int]) -> bool:
    return status in {400, 401, 403, 404, 405, 409, 415, 422, 429}


def add_authz_tests(base_url: str, timeout: float) -> list[Finding]:
    tests = [
        ("匿名写接口", "POST", "/api/route-optimization/apply", {"Content-Type": "application/json"},
         '{"routeId":1,"optimizationType":"distance"}', "high"),
        ("伪造内部用户头", "POST", "/api/route-optimization/apply",
         {"Content-Type": "application/json", "X-User-Id": "1", "X-User-Role": "ADMIN"},
         '{"routeId":1,"optimizationType":"distance"}', "critical"),
        ("畸形 JWT", "GET", "/api/v1/notifications", {"Authorization": "Bearer not.a.valid.jwt"}, None, "high"),
        ("算法混淆 JWT", "GET", "/api/v1/notifications",
         {"Authorization": "Bearer eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJ1c2VySWQiOjEsInVzZXJUeXBlIjo5fQ."}, None, "critical"),
        ("Swagger 公开暴露", "GET", "/v3/api-docs", {}, None, "medium"),
        ("Actuator 公开暴露", "GET", "/actuator/env", {}, None, "critical"),
    ]
    findings: list[Finding] = []
    for name, method, path, headers, body, severity in tests:
        status, response_headers, payload, elapsed = request(base_url, method, path, timeout, headers, body)
        if name == "Swagger 公开暴露":
            passed = status not in {200, 301, 302}
            rec = "生产环境关闭或鉴权保护 Swagger/OpenAPI；至少不要通过公网 Gateway 暴露。"
        elif name == "Actuator 公开暴露":
            passed = status not in {200, 301, 302}
            rec = "Actuator 仅暴露 health/info；敏感端点绑定管理网口并要求鉴权。"
        else:
            passed = status_is_denied(status)
            rec = "确保 Gateway 与业务服务双层鉴权；业务服务不可依赖可伪造的转发头。"
        evidence = f"HTTP {status}; body={payload[:240]!r}"
        findings.append(Finding(name, severity, passed, method, path, status, evidence, rec, elapsed))
    return findings


def add_input_tests(base_url: str, timeout: float) -> list[Finding]:
    payloads = [
        ("SQL 注入型搜索输入", "/api/routes/search?keyword=%27%20OR%201%3D1--", "high"),
        ("XSS 型搜索输入", "/api/routes/search?keyword=%3Cscript%3Ealert%281%29%3C%2Fscript%3E", "medium"),
        ("超长查询输入", "/api/routes/search?keyword=" + "A" * 4096, "medium"),
        ("路径穿越型资源请求", "/api/resource-file/content/..%2F..%2F..%2Fetc%2Fpasswd", "high"),
    ]
    findings: list[Finding] = []
    for name, path, severity in payloads:
        status, headers, payload, elapsed = request(base_url, "GET", path, timeout)
        server_error = status is not None and status >= 500
        leaked = any(marker in payload.lower() for marker in (
            "sqlsyntax", "bad sql", "stacktrace", "java.lang.", "root:x:0:0", "syntax error"
        ))
        passed = not server_error and not leaked
        evidence = f"HTTP {status}; leaked={leaked}; body={payload[:240]!r}"
        findings.append(Finding(
            name, severity, passed, "GET", path[:160], status, evidence,
            "对字符串设置长度上限；使用参数化查询；统一异常响应禁止返回 SQL、栈和本机路径。", elapsed
        ))
    return findings


def add_header_tests(base_url: str, timeout: float) -> list[Finding]:
    status, headers, payload, elapsed = request(base_url, "GET", "/api/routes", timeout)
    missing = []
    for name, expected in SECURITY_HEADERS.items():
        value = headers.get(name)
        if value is None or (expected is not None and expected.lower() not in value.lower()):
            missing.append(name)
    disclosed = {name: headers[name] for name in SENSITIVE_HEADERS if name in headers}
    findings = [Finding(
        "安全响应头", "medium", not missing, "GET", "/api/routes", status,
        f"missing={missing}; present={sorted(headers)}",
        "在 Gateway 统一添加 CSP、X-Content-Type-Options、X-Frame-Options、Referrer-Policy、Permissions-Policy。",
        elapsed,
    )]
    findings.append(Finding(
        "服务指纹泄露", "low", not disclosed, "GET", "/api/routes", status,
        f"disclosed={disclosed}", "移除 Server、X-Powered-By、X-Application-Context 等指纹头。", elapsed
    ))
    return findings


def add_cors_test(base_url: str, timeout: float) -> list[Finding]:
    evil_origin = "https://evil.example"
    status, headers, payload, elapsed = request(
        base_url, "OPTIONS", "/api/route-optimization/apply", timeout,
        headers={
            "Origin": evil_origin,
            "Access-Control-Request-Method": "POST",
            "Access-Control-Request-Headers": "authorization,content-type",
        },
    )
    allow_origin = headers.get("access-control-allow-origin", "")
    allow_credentials = headers.get("access-control-allow-credentials", "").lower() == "true"
    dangerous = allow_origin == "*" and allow_credentials
    reflected = allow_origin == evil_origin
    passed = not dangerous and not reflected
    return [Finding(
        "恶意 Origin CORS 预检", "high", passed, "OPTIONS", "/api/route-optimization/apply", status,
        f"allow-origin={allow_origin!r}; allow-credentials={allow_credentials}; body={payload[:120]!r}",
        "CORS 使用明确域名白名单；禁止反射任意 Origin，凭据模式不得搭配通配符。", elapsed
    )]


def add_public_ai_policy_test(base_url: str, timeout: float) -> list[Finding]:
    # 只发 GET 到一个应认证的 AI 优化端点，避免触发任何大模型调用。
    path = "/api/ai/assistant/optimize/1"
    status, headers, payload, elapsed = request(base_url, "GET", path, timeout)
    passed = status_is_denied(status)
    return [Finding(
        "AI 路线优化端点匿名访问", "critical", passed, "GET", path, status,
        f"HTTP {status}; body={payload[:240]!r}",
        "移除 Gateway 对 /api/ai/** 的整体放行；按端点分级鉴权，并对生成式 AI 设置用户/IP 配额、并发和日预算。",
        elapsed,
    )]


def add_direct_service_tests(base_url: str, timeout: float) -> list[Finding]:
    parsed = urlparse(base_url)
    scheme = parsed.scheme
    host = parsed.hostname or "127.0.0.1"
    findings: list[Finding] = []
    probes = [(8091, "/actuator/health"), (8092, "/actuator/health"), (8093, "/actuator/health"),
              (8094, "/actuator/health"), (8095, "/actuator/health")]
    for port, path in probes:
        target = f"{scheme}://{host}:{port}"
        status, headers, payload, elapsed = request(target, "GET", path, timeout)
        reachable = status is not None
        findings.append(Finding(
            f"业务服务端口 {port} 直连", "high", not reachable, "GET", f"{target}{path}", status,
            f"reachable={reachable}; HTTP {status}; body={payload[:160]!r}",
            "生产部署仅暴露 Gateway；MySQL、Redis 和 8091-8095 使用 expose/内部网络，不映射宿主公网端口。",
            elapsed,
        ))
    return findings


def add_rate_limit_test(base_url: str, timeout: float) -> list[Finding]:
    def one(_: int) -> Optional[int]:
        return request(base_url, "GET", "/api/routes", timeout)[0]
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as executor:
        statuses = list(executor.map(one, range(12)))
    limited = any(status == 429 for status in statuses)
    return [Finding(
        "轻量限流探测（12 请求）", "medium", limited, "GET", "/api/routes", None,
        f"statuses={statuses}",
        "在 Gateway 按 IP+用户实施令牌桶；登录、公开统计、AI 和文件下载使用独立更严格限额。",
    )]


def summarize(findings: Iterable[Finding]) -> dict:
    items = list(findings)
    return {
        "total": len(items),
        "passed": sum(item.passed for item in items),
        "failed": sum(not item.passed for item in items),
        "failed_by_severity": {
            severity: sum((not item.passed) and item.severity == severity for item in items)
            for severity in ("critical", "high", "medium", "low")
        },
    }


def main() -> int:
    args = parse_args()
    validate_target(args.base_url, args.allow_private_target)
    findings: list[Finding] = []
    findings.extend(add_authz_tests(args.base_url, args.timeout))
    findings.extend(add_input_tests(args.base_url, args.timeout))
    findings.extend(add_header_tests(args.base_url, args.timeout))
    findings.extend(add_cors_test(args.base_url, args.timeout))
    findings.extend(add_public_ai_policy_test(args.base_url, args.timeout))
    if args.check_direct_services:
        findings.extend(add_direct_service_tests(args.base_url, args.timeout))
    if args.check_rate_limit:
        findings.extend(add_rate_limit_test(args.base_url, args.timeout))

    report = {
        "tool": "travel-local-security-probe",
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "target": args.base_url,
        "safety": {
            "localhost_only_by_default": True,
            "destructive_actions": False,
            "max_concurrent_requests": 4 if args.check_rate_limit else 1,
            "real_ai_generation_called": False,
        },
        "summary": summarize(findings),
        "findings": [asdict(item) for item in findings],
    }
    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")

    print(json.dumps(report["summary"], ensure_ascii=False, indent=2))
    print(f"报告已写入: {output.resolve()}")
    for item in findings:
        mark = "PASS" if item.passed else "FAIL"
        print(f"[{mark}] [{item.severity.upper()}] {item.test}: {item.evidence[:160]}")
    return 1 if report["summary"]["failed"] else 0


if __name__ == "__main__":
    sys.exit(main())

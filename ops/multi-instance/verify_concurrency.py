#!/usr/bin/env python3
"""collection-service 双实例一致性验证脚本。

验证目标：Gateway 负载均衡把请求分发到两个 collection 副本时，
HTTP 幂等状态机 + MySQL 唯一键依然保证"1 成功 + 其余冲突/重放，最终 1 条记录"。

用法：
  python ops/multi-instance/verify_concurrency.py --base-url http://127.0.0.1:8090 \
      --token <JWT> --route-id 1 --concurrency 20

安全约束：仅允许 localhost / RFC1918 私网；只打收藏幂等接口，不做破坏性操作。
"""
from __future__ import annotations

import argparse
import concurrent.futures
import http.client
import ipaddress
import json
import sys
import time
from collections import Counter
from urllib.parse import urlparse

LOCAL_HOSTS = {"127.0.0.1", "localhost", "::1"}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="多实例幂等一致性验证")
    parser.add_argument("--base-url", default="http://127.0.0.1:8090")
    parser.add_argument("--token", required=True, help="已登录用户的 JWT（Bearer）")
    parser.add_argument("--route-id", type=int, default=1, help="目标路线 ID")
    # 幂等键按时间戳生成，同一次运行内并发共享
    parser.add_argument("--concurrency", type=int, default=20, help="并发数（默认 20，上限 100）")
    parser.add_argument("--timeout", type=float, default=6.0)
    parser.add_argument("--output", default="ops/multi-instance/verify-result.json")
    return parser.parse_args()


def validate_target(base_url: str) -> None:
    host = urlparse(base_url).hostname or ""
    if host in LOCAL_HOSTS:
        return
    try:
        if ipaddress.ip_address(host).is_private:
            return
    except ValueError:
        pass
    raise SystemExit("安全保护：仅允许 localhost / 私网目标")


def main() -> int:
    args = parse_args()
    validate_target(args.base_url)
    n = max(2, min(args.concurrency, 100))

    # 同一幂等键并发打 N 次：期望 1 个真实成功，其余 409/重放
    idem_key = f"multi-instance-verify-{args.route_id}-{int(time.time())}"

    def call(_: int) -> tuple[int, str, str]:
        parsed = urlparse(args.base_url)
        conn = http.client.HTTPConnection(parsed.hostname, parsed.port, timeout=args.timeout)
        body = json.dumps({"routeId": args.route_id, "collectionType": 1})
        headers = {
            "Authorization": f"Bearer {args.token}",
            "Content-Type": "application/json",
            "Idempotency-Key": idem_key,
        }
        try:
            conn.request("POST", "/api/v1/route-collections", body=body, headers=headers)
            resp = conn.getresponse()
            payload = resp.read(4096).decode("utf-8", errors="replace")
            return resp.status, payload, (resp.getheader("Idempotency-Replayed") or "")
        except (OSError, http.client.HTTPException) as exc:
            return -1, f"{type(exc).__name__}: {exc}", ""
        finally:
            conn.close()

    started = time.perf_counter()
    with concurrent.futures.ThreadPoolExecutor(max_workers=n) as pool:
        results = list(pool.map(call, range(n)))
    elapsed_ms = int((time.perf_counter() - started) * 1000)

    statuses = Counter(status for status, _, _ in results)
    replayed = sum(1 for _, _, flag in results if flag.lower() == "true")
    passed = statuses.get(200, 0) >= 1 and statuses.get(409, 0) + replayed == n - statuses.get(-1, 0)

    report = {
        "test": "multi-instance idempotency consistency",
        "target": args.base_url,
        "idempotency_key": idem_key,
        "concurrency": n,
        "elapsed_ms": elapsed_ms,
        "status_distribution": dict(statuses),
        "replayed_header_count": replayed,
        "expected": "1 个 200（真实写入），其余 409 或重放；DB 仅 1 条记录（需人工核库）",
        "passed_transport_layer": passed,
        "note": "传输层判定通过后，需人工核对 user_collection 仅 1 条记录，"
                "并在两实例日志中确认请求确实分布到了两个副本（grep 同 Idempotency-Key）",
    }
    with open(args.output, "w", encoding="utf-8") as fh:
        json.dump(report, fh, ensure_ascii=False, indent=2)
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0 if passed else 1


if __name__ == "__main__":
    sys.exit(main())

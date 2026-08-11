#!/usr/bin/env python3
import argparse
import json
import time
import sys
import urllib.error
import urllib.parse
import urllib.request


DEFAULT_BASES = {
    "user": "http://127.0.0.1:8091/api",
    "attraction": "http://127.0.0.1:8092/api",
    "route": "http://127.0.0.1:8093/api",
    "collection": "http://127.0.0.1:8094/api",
    "file": "http://127.0.0.1:8095/api",
}


def configure_output_encoding():
    for stream in (sys.stdout, sys.stderr):
        reconfigure = getattr(stream, "reconfigure", None)
        if reconfigure is not None:
            reconfigure(encoding="utf-8")


def request_json(method, url, data=None, headers=None, timeout=20):
    headers = dict(headers or {})
    body = None
    if data is not None:
        body = json.dumps(data).encode("utf-8")
        headers["Content-Type"] = "application/json"

    request = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            payload_text = response.read().decode("utf-8", errors="replace")
            try:
                payload = json.loads(payload_text)
            except Exception:
                payload = payload_text
            return response.status, payload
    except urllib.error.HTTPError as error:
        payload_text = error.read().decode("utf-8", errors="replace")
        try:
            payload = json.loads(payload_text)
        except Exception:
            payload = payload_text
        return error.code, payload
    except Exception as error:
        return None, {"error": str(error)}


def wait_for_ready(url, timeout_seconds, poll_interval):
    deadline = time.time() + timeout_seconds
    while time.time() < deadline:
        status, payload = request_json("GET", url, timeout=poll_interval)
        if status == 200 and isinstance(payload, dict):
            return True
        time.sleep(poll_interval)
    return False


def ok(result):
    status, payload = result
    if status != 200 or not isinstance(payload, dict):
        return False
    return payload.get("code") == 200


def build_args():
    parser = argparse.ArgumentParser(description="Travel API smoke test")
    parser.add_argument("--username", default="zhangsan")
    parser.add_argument("--password", default="123456")
    parser.add_argument("--timeout", type=int, default=20)
    parser.add_argument("--wait-seconds", type=int, default=120)
    parser.add_argument("--poll-interval", type=int, default=2)
    for name, default in DEFAULT_BASES.items():
        parser.add_argument(f"--{name}-base", default=default)
    return parser.parse_args()


def main():
    configure_output_encoding()
    args = build_args()

    ready_urls = [f"{base}/v3/api-docs" for base in (
        args.user_base,
        args.attraction_base,
        args.route_base,
        args.collection_base,
        args.file_base,
    )]
    for url in ready_urls:
        if not wait_for_ready(url, args.wait_seconds, args.poll_interval):
            print(json.dumps({"step": "wait", "url": url, "error": "service not ready"}, ensure_ascii=False, indent=2))
            return 1

    login_url = f"{args.user_base}/users/login"
    login = request_json(
        "POST",
        login_url,
        data={"username": args.username, "password": args.password},
        timeout=args.timeout,
    )
    if not ok(login):
        print(json.dumps({"step": "login", "result": login}, ensure_ascii=False, indent=2))
        return 1

    token = login[1]["data"]["token"]
    auth = {"Authorization": f"Bearer {token}"}

    tests = {
        "user.current": ("GET", f"{args.user_base}/users/current", None),
        "collection.notifications": ("GET", f"{args.collection_base}/v1/notifications", None),
        "collection.unread_count": ("GET", f"{args.collection_base}/v1/notifications/unread-count", None),
        "collection.user_stats": ("GET", f"{args.collection_base}/v1/user/stats", None),
        "collection.route_share_validate_code": ("GET", f"{args.collection_base}/route-share/validate?code=qwe456rty789", None),
        "collection.route_share_validate_shareCode": ("GET", f"{args.collection_base}/route-share/validate?shareCode=qwe456rty789", None),
        "file.category_list": ("GET", f"{args.file_base}/resource-file/category/list", None),
        "file.search": ("GET", f"{args.file_base}/resource-file/search?keyword=jpg&page=0&size=10", None),
        "route.real_time_adjustment": ("POST", f"{args.route_base}/routes/smart/real-time-adjustment/1", {}),
    }

    results = {}
    for name, (method, url, body) in tests.items():
        result = request_json(method, url, data=body, headers=auth, timeout=args.timeout)
        results[name] = {
            "http": result[0],
            "payload": result[1],
            "ok": ok(result),
        }

    failures = {name: value for name, value in results.items() if not value["ok"]}
    print(json.dumps(results, ensure_ascii=False, indent=2))
    return 0 if not failures else 2


if __name__ == "__main__":
    sys.exit(main())

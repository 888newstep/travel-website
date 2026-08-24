#!/usr/bin/env python3
import base64
import hashlib
import hmac
import json
import os
import pathlib
import subprocess
import sys
import time
import uuid
from datetime import datetime, timezone

import requests


REPO_ROOT = pathlib.Path(__file__).resolve().parents[2]
BASE_URL = os.getenv("E2E_BASE_URL", "http://127.0.0.1:8090/api")
PASSWORD = os.getenv("TEST_DATA_USER_PASSWORD", "123456")
USER_A = os.getenv("E2E_USER_A", "zhangsan")
USER_B = os.getenv("E2E_USER_B", "lisi")
ADMIN_USER = os.getenv("E2E_ADMIN_USER", "guoxia")


def read_dotenv(path: pathlib.Path) -> dict[str, str]:
    values: dict[str, str] = {}
    if not path.is_file():
        return values
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        name, value = line.split("=", 1)
        value = value.strip()
        if len(value) >= 2 and value[0] == value[-1] and value[0] in "\"'":
            value = value[1:-1]
        values[name.strip()] = value
    return values


class AcceptanceRunner:
    def __init__(self) -> None:
        timestamp = datetime.now().strftime("%Y%m%d-%H%M%S")
        self.output_dir = REPO_ROOT / "run-logs" / "e2e-acceptance" / timestamp
        self.output_dir.mkdir(parents=True, exist_ok=True)
        self.session = requests.Session()
        self.results: list[dict] = []
        self.cleanup_actions: list[tuple[str, callable]] = []
        self.tokens: dict[str, str] = {}
        self.env = read_dotenv(REPO_ROOT / "deploy" / ".env")
        self.admin_original_type: int | None = None

    def record(self, name: str, passed: bool, **details) -> None:
        result = {"name": name, "passed": passed, **details}
        self.results.append(result)
        print(json.dumps(result, ensure_ascii=False))
        if not passed:
            raise AssertionError(f"Acceptance check failed: {name}: {details}")

    def request(
        self,
        name: str,
        method: str,
        path: str,
        *,
        token: str | None = None,
        expected_status: int = 200,
        expected_code: int | None = 200,
        **kwargs,
    ) -> tuple[requests.Response, dict]:
        headers = dict(kwargs.pop("headers", {}))
        if token:
            headers["Authorization"] = f"Bearer {token}"
        response = self.session.request(
            method,
            BASE_URL + path,
            headers=headers,
            timeout=45,
            **kwargs,
        )
        try:
            body = response.json()
        except ValueError:
            body = {}
        actual_code = body.get("code")
        passed = response.status_code == expected_status
        if expected_code is not None:
            passed = passed and actual_code == expected_code
        self.record(
            name,
            passed,
            status=response.status_code,
            code=actual_code,
            replayed=response.headers.get("Idempotency-Replayed"),
            message=body.get("message"),
        )
        return response, body

    def mysql(self, sql: str) -> str:
        mysql = os.getenv(
            "MYSQL_PATH",
            r"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
        )
        db_password = self.env.get("DB_PASSWORD", "")
        if not pathlib.Path(mysql).is_file() or not db_password:
            raise RuntimeError("mysql.exe and DB_PASSWORD are required for the admin matrix")
        command = [
            mysql,
            "--host=127.0.0.1",
            "--port=3306",
            "--user=root",
            "--database=travel_website",
            "--batch",
            "--raw",
            "--skip-column-names",
            f"--execute={sql}",
        ]
        environment = os.environ.copy()
        environment["MYSQL_PWD"] = db_password
        completed = subprocess.run(
            command,
            cwd=REPO_ROOT,
            env=environment,
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
            timeout=30,
            check=False,
        )
        if completed.returncode != 0:
            raise RuntimeError(f"MySQL command failed: {completed.stderr.strip()}")
        return completed.stdout.strip()

    def login(self, username: str) -> str:
        _, body = self.request(
            f"login-{username}",
            "POST",
            "/users/login",
            json={"username": username, "password": PASSWORD},
        )
        token = (body.get("data") or {}).get("token")
        self.record(f"token-{username}", bool(token), tokenPresent=bool(token))
        return token

    def expired_token(self) -> str:
        jwt_secret = self.env.get("JWT_SECRET", "")
        if not jwt_secret:
            raise RuntimeError("JWT_SECRET is required for the expired JWT matrix")

        def encode(value: bytes) -> str:
            return base64.urlsafe_b64encode(value).decode("ascii").rstrip("=")

        header = encode(json.dumps({"alg": "HS512"}, separators=(",", ":")).encode("utf-8"))
        payload = encode(
            json.dumps(
                {
                    "sub": "TRAVEL-PLATFORM-USER",
                    "exp": int(time.time()) - 60,
                    "iat": int(time.time()) - 3600,
                    "jti": "1",
                    "userId": 1,
                    "userType": 1,
                },
                separators=(",", ":"),
            ).encode("utf-8")
        )
        signing_key = base64.b64encode(jwt_secret.encode("utf-8"))
        signature = hmac.new(
            signing_key, f"{header}.{payload}".encode("ascii"), hashlib.sha512
        ).digest()
        return f"{header}.{payload}.{encode(signature)}"

    def prepare_admin(self) -> None:
        original = self.mysql(
            "SELECT user_type FROM user WHERE username='" + ADMIN_USER.replace("'", "''") + "' LIMIT 1;"
        )
        if not original.isdigit():
            raise RuntimeError(f"Admin fixture user was not found: {ADMIN_USER}")
        self.admin_original_type = int(original)
        self.mysql(
            "UPDATE user SET user_type=9 WHERE username='" + ADMIN_USER.replace("'", "''") + "';"
        )

    def restore_admin(self) -> None:
        if self.admin_original_type is None:
            return
        self.mysql(
            "UPDATE user SET user_type="
            + str(self.admin_original_type)
            + " WHERE username='"
            + ADMIN_USER.replace("'", "''")
            + "';"
        )
        self.admin_original_type = None

    def defer_cleanup(self, name: str, action) -> None:
        self.cleanup_actions.append((name, action))

    def run(self) -> None:
        health = self.session.get("http://127.0.0.1:8090/actuator/health", timeout=15)
        self.record("health-gateway", health.status_code == 200, status=health.status_code)
        self.request("anonymous-cities", "GET", "/cities")
        self.request("anonymous-attractions", "GET", "/attractions", params={"current": 1, "size": 3})
        self.request("anonymous-route-search", "GET", "/routes/search", params={"title": "北京"})
        self.request(
            "amap-place-search",
            "GET",
            "/attractions/external-search",
            params={"keyword": "故宫", "city": "北京"},
        )
        self.request("amap-weather", "GET", "/attractions/1/weather")
        self.request(
            "amap-nearby",
            "GET",
            "/attractions/1/nearby-facilities",
            params={"category": "restaurant", "radiusMeters": 1000},
        )

        token_a = self.login(USER_A)
        token_b = self.login(USER_B)
        self.tokens.update(a=token_a, b=token_b)
        self.request("current-user", "GET", "/users/current", token=token_a)

        self.request(
            "missing-jwt",
            "POST",
            "/routes",
            expected_status=401,
            expected_code=None,
            json={"title": "blocked", "cityId": 1, "durationDays": 1},
        )
        self.request(
            "invalid-jwt",
            "GET",
            "/users/current",
            expected_status=401,
            expected_code=None,
            headers={"Authorization": "Bearer invalid.jwt.value"},
        )
        self.request(
            "expired-jwt",
            "GET",
            "/users/current",
            expected_status=401,
            expected_code=None,
            headers={"Authorization": "Bearer " + self.expired_token()},
        )
        self.request(
            "forged-identity-header",
            "GET",
            "/users/current",
            expected_status=401,
            expected_code=None,
            headers={"X-User-Id": "1", "X-User-Type": "9", "X-User-Role": "ROLE_ADMIN"},
        )

        idempotency_key = "route-" + uuid.uuid4().hex
        route_payload = {
            "title": "E2E acceptance " + uuid.uuid4().hex[:8],
            "description": "Automated full business acceptance",
            "cityId": 1,
            "durationDays": 1,
            "difficulty": "easy",
        }
        _, created = self.request(
            "route-create",
            "POST",
            "/routes",
            token=token_a,
            headers={"Idempotency-Key": idempotency_key},
            json=route_payload,
        )
        route_id = (created.get("data") or {}).get("id")
        self.record("route-id", isinstance(route_id, int), routeId=route_id)
        self.defer_cleanup(
            "route",
            lambda: self.session.delete(
                BASE_URL + f"/routes/{route_id}",
                headers={"Authorization": f"Bearer {token_a}"},
                timeout=30,
            ),
        )

        replay_response, replay_body = self.request(
            "route-create-replay",
            "POST",
            "/routes",
            token=token_a,
            headers={"Idempotency-Key": idempotency_key},
            json=route_payload,
        )
        self.record(
            "route-replay-header-and-id",
            replay_response.headers.get("Idempotency-Replayed") == "true"
            and (replay_body.get("data") or {}).get("id") == route_id,
            routeId=route_id,
        )
        self.request(
            "route-create-conflict",
            "POST",
            "/routes",
            token=token_a,
            expected_status=409,
            expected_code=409,
            headers={"Idempotency-Key": idempotency_key},
            json={**route_payload, "title": route_payload["title"] + " changed"},
        )
        self.request(
            "anonymous-draft-route",
            "GET",
            f"/routes/{route_id}",
            expected_status=403,
            expected_code=2002,
        )
        self.request(
            "cross-user-draft-route",
            "GET",
            f"/routes/{route_id}",
            token=token_b,
            expected_status=403,
            expected_code=2002,
        )
        self.request(
            "cross-user-route-update",
            "PUT",
            f"/routes/{route_id}",
            token=token_b,
            expected_status=403,
            expected_code=2002,
            json={"title": "forbidden"},
        )

        self.request(
            "route-schedule",
            "PUT",
            f"/routes/{route_id}/schedule",
            token=token_a,
            json={
                "items": [
                    {"attractionId": 1, "dayNumber": 1, "visitOrder": 1, "notes": "Palace"},
                    {"attractionId": 2, "dayNumber": 1, "visitOrder": 2, "notes": "Square"},
                ]
            },
        )
        self.request("route-publish", "POST", f"/routes/{route_id}/publish", token=token_a)
        self.request("anonymous-published-route", "GET", f"/routes/{route_id}")
        self.request("anonymous-published-schedule", "GET", f"/routes/{route_id}/schedule")
        _, traffic = self.request("anonymous-route-traffic", "GET", f"/routes/{route_id}/traffic")
        self.record(
            "route-traffic-contract",
            isinstance((traffic.get("data") or {}).get("dataAvailable"), bool),
            source=(traffic.get("data") or {}).get("source"),
        )

        self.request(
            "route-collection-on",
            "POST",
            "/v1/route-collections/toggle",
            token=token_b,
            json={"routeId": route_id},
        )
        self.defer_cleanup(
            "collection",
            lambda: self.session.delete(
                BASE_URL + "/v1/route-collections/remove",
                headers={"Authorization": f"Bearer {token_b}"},
                params={"routeId": route_id},
                timeout=30,
            ),
        )
        self.request(
            "route-collection-check",
            "GET",
            "/v1/route-collections/check",
            token=token_b,
            params={"routeId": route_id},
        )

        _, comment = self.request(
            "route-comment-create",
            "POST",
            "/route-comments",
            token=token_b,
            json={
                "routeId": route_id,
                "rating": 4.5,
                "content": "Acceptance comment",
                "isAnonymous": False,
            },
        )
        comment_id = (comment.get("data") or {}).get("id")
        self.record("comment-id", isinstance(comment_id, int), commentId=comment_id)
        self.defer_cleanup(
            "comment",
            lambda: self.session.delete(
                BASE_URL + f"/route-comments/{comment_id}",
                headers={"Authorization": f"Bearer {token_b}"},
                timeout=30,
            ),
        )
        self.request(
            "comment-like-on", "POST", f"/route-comments/{comment_id}/toggle-like", token=token_a
        )
        self.request(
            "comment-like-off", "POST", f"/route-comments/{comment_id}/toggle-like", token=token_a
        )

        _, note = self.request(
            "travel-note-create",
            "POST",
            "/travel-notes",
            token=token_a,
            json={
                "travelNote": {
                    "title": "E2E note " + uuid.uuid4().hex[:6],
                    "content": "Full business acceptance content",
                    "cityId": 1,
                    "isPublic": True,
                },
                "tags": ["e2e", "campus-recruiting"],
            },
        )
        note_id = (note.get("data") or {}).get("id")
        self.record("note-id", isinstance(note_id, int), noteId=note_id)
        self.defer_cleanup(
            "note",
            lambda: self.session.delete(
                BASE_URL + f"/travel-notes/{note_id}",
                headers={"Authorization": f"Bearer {token_a}"},
                timeout=30,
            ),
        )
        self.request("anonymous-note-view", "POST", f"/travel-notes/{note_id}/view")
        self.request("note-like", "POST", f"/travel-notes/{note_id}/toggle-like", token=token_b)
        self.request("note-collect", "POST", f"/travel-notes/{note_id}/toggle-collect", token=token_b)
        self.request(
            "note-update",
            "PUT",
            f"/travel-notes/{note_id}",
            token=token_a,
            json={
                "travelNote": {
                    "title": "E2E note updated",
                    "content": "Updated acceptance content",
                    "cityId": 1,
                    "isPublic": True,
                },
                "tags": ["e2e"],
            },
        )
        self.request("anonymous-note-detail", "GET", f"/travel-notes/{note_id}")

        _, share = self.request(
            "route-share-generate",
            "POST",
            "/route-share/generate",
            token=token_a,
            json={"itemId": route_id, "itemType": "route"},
        )
        share_data = share.get("data") or {}
        share_id = share_data.get("id")
        share_code = share_data.get("shareCode")
        self.record(
            "share-identifiers",
            isinstance(share_id, int) and bool(share_code),
            shareId=share_id,
            shareCodePresent=bool(share_code),
        )
        self.defer_cleanup(
            "share",
            lambda: self.session.delete(
                BASE_URL + f"/route-share/cancel/{share_id}",
                headers={"Authorization": f"Bearer {token_a}"},
                timeout=30,
            ),
        )
        self.request(
            "share-validate", "GET", "/route-share/validate", params={"code": share_code}
        )
        self.request("share-access", "GET", f"/route-share/access/{share_code}")
        self.request("share-visit", "POST", f"/route-share/visit/{share_code}")
        self.request(
            "share-statistics", "GET", f"/route-share/statistics/{share_id}", token=token_a
        )
        self.request(
            "cross-user-share-statistics",
            "GET",
            f"/route-share/statistics/{share_id}",
            token=token_b,
            expected_status=403,
            expected_code=2002,
        )

        upload_path = self.output_dir / "e2e-upload.txt"
        upload_path.write_text("travel e2e upload", encoding="utf-8")
        with upload_path.open("rb") as upload_file:
            _, uploaded = self.request(
                "file-upload",
                "POST",
                "/resource-file/upload",
                token=token_a,
                files={"file": (upload_path.name, upload_file, "text/plain")},
                data={"category": "e2e", "description": "full acceptance"},
            )
        file_id = (uploaded.get("data") or {}).get("id")
        self.record("file-id", isinstance(file_id, int), fileId=file_id)
        self.defer_cleanup(
            "file",
            lambda: self.session.delete(
                BASE_URL + f"/resource-file/delete/{file_id}",
                headers={"Authorization": f"Bearer {token_a}"},
                timeout=30,
            ),
        )
        self.request("file-owner-detail", "GET", f"/resource-file/{file_id}", token=token_a)
        self.request(
            "file-cross-user-detail",
            "GET",
            f"/resource-file/{file_id}",
            token=token_b,
            expected_status=403,
            expected_code=9009,
        )
        file_response = self.session.get(
            BASE_URL + f"/resource-file/content/{file_id}",
            headers={"Authorization": f"Bearer {token_a}"},
            timeout=30,
        )
        self.record(
            "file-content",
            file_response.status_code == 200 and file_response.content == b"travel e2e upload",
            status=file_response.status_code,
            bytes=len(file_response.content),
        )

        self.request("notifications", "GET", "/v1/notifications", token=token_a)
        self.request("notification-unread-count", "GET", "/v1/notifications/unread-count", token=token_a)
        self.request("user-statistics", "GET", "/v1/user/stats", token=token_a)

        self.prepare_admin()
        try:
            admin_token = self.login(ADMIN_USER)
        finally:
            self.restore_admin()
        self.request(
            "normal-user-admin-endpoint",
            "GET",
            "/feedback/statistics",
            token=token_a,
            expected_status=403,
            expected_code=None,
        )
        self.request("admin-feedback-statistics", "GET", "/feedback/statistics", token=admin_token)

    def cleanup(self) -> list[dict]:
        cleanup_results = []
        for name, action in reversed(self.cleanup_actions):
            try:
                response = action()
                cleanup_results.append(
                    {"name": name, "status": response.status_code, "success": response.status_code < 500}
                )
            except Exception as exception:
                cleanup_results.append({"name": name, "success": False, "error": str(exception)})
        try:
            self.restore_admin()
        except Exception as exception:
            cleanup_results.append({"name": "admin-restore", "success": False, "error": str(exception)})
        return cleanup_results

    def write_summary(self, success: bool, error: str | None, cleanup_results: list[dict]) -> None:
        summary = {
            "generatedAt": datetime.now(timezone.utc).astimezone().isoformat(),
            "success": success,
            "baseUrl": BASE_URL,
            "checks": self.results,
            "passed": sum(1 for result in self.results if result["passed"]),
            "failed": sum(1 for result in self.results if not result["passed"]),
            "error": error,
            "cleanup": cleanup_results,
        }
        (self.output_dir / "run-summary.json").write_text(
            json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8"
        )
        print(f"Summary: {self.output_dir / 'run-summary.json'}")


def main() -> int:
    runner = AcceptanceRunner()
    success = False
    error = None
    try:
        runner.run()
        success = True
    except Exception as exception:
        error = f"{type(exception).__name__}: {exception}"
        print(error, file=sys.stderr)
    finally:
        cleanup_results = runner.cleanup()
        runner.write_summary(success, error, cleanup_results)
    return 0 if success else 1


if __name__ == "__main__":
    raise SystemExit(main())

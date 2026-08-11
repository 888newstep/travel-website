# Project Conventions
- Keep edits scoped; the worktree may contain user/agent changes and generated `target/` deletions. Never reset or checkout unrelated files.
- Mapper interfaces live in `intern-base-intf`; SQL behavior is defined by corresponding XML/resources, and integration tests should load production XML rather than duplicate SQL logic.
- Outbox status transitions use conditional updates on `status`, `retry_count`, `next_attempt_time`, and `lease_until`; relay claims are database-coordinated and lease expiry enables recovery.
- Business side effects and outbox insertion belong in the same transaction; `DISPATCHED` means local RabbitTemplate acceptance, not broker confirmation or consumer success.
- Side-effecting controllers accept optional `Idempotency-Key` for backward compatibility; Redis stores request fingerprint and terminal response with Lua ownership transitions.
- Consumer/listener MDC is scoped with finally-based restoration; token-based compare-and-delete is required for Redis lock release.
- Tests that require Redis/MySQL/RabbitMQ extend container base classes and use `*IT.java`; no production dependency should be introduced for test-only infrastructure.

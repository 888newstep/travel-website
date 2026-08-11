# Project Core
- Multi-module Maven project: `intern-base-intf` (DTO/entity/mapper/SQL), `intern-base-service` (business, cache, MQ, outbox), `intern-base-web` (Spring Boot web/security/config).
- Root architecture remains a modular monolith; do not infer microservice boundaries without traffic/deployment evidence.
- Primary reliability chain: HTTP filters/validation -> service transaction + lock/idempotency -> MySQL business write + `mq_outbox` in one transaction -> relay claim/lease -> RabbitMQ publish/confirm/ack -> consumer idempotency/status update -> Micrometer/MDC.
- Durable design invariant: database unique constraints are final correctness boundary; Redis locks/idempotency and MQ delivery are at-least-once coordination mechanisms, not correctness substitutes.
- Focused references: `mem:tech_stack` for versions/dependencies; `mem:suggested_commands` for Windows/Maven commands; `mem:conventions` for code patterns; `mem:task_completion` for completion checks.

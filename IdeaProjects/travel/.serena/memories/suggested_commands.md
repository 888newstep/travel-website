# Commands
- Run from the Windows project root `C:\Users\xiaohongfu\IdeaProjects\xiaozhaovip-intern8`.
- Fast source/test compilation: `mvn -q -DskipTests compile` and `mvn -q -DskipTests test-compile`.
- Unit tests without external services: `mvn -q test`; `*IT.java` is not included by default.
- Real-component path: start `docker compose up -d mysql redis rabbitmq`, then run `mvn -q -Pintegration-test verify`, and clean with `docker compose down -v`.
- Integration profile deliberately fails with `Docker CLI or daemon is unavailable` when Docker is absent; do not reinterpret that as a passing or skipped component test.
- Windows search: prefer `rg -n -C <N> <pattern> <path>` and `rg --files`; use PowerShell `Get-Content` for focused reads.

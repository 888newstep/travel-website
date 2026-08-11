# Completion Checks
- For source-only changes: run `mvn -q -DskipTests compile` and `mvn -q -DskipTests test-compile`.
- For behavior changes: run `mvn -q test` and inspect the Surefire XML totals; current baseline is 57 unit tests with zero failures/errors.
- For integration changes: run `mvn -q -Pintegration-test verify` with Docker. A no-Docker result is an explicit environment failure and must remain `[~]` in `tech-deepening-checklist.md`.
- Run targeted `git diff --check` for touched files; pre-existing whitespace findings in unrelated files should be reported, not fixed opportunistically.
- Keep `tech-deepening-checklist.md` status aligned with evidence: `[x]` only for executed/verified behavior, `[~]` for implemented but infrastructure-unverified behavior.

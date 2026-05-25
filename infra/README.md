# infra

- `flyway/` — reserved for environment-specific Flyway configuration (e.g. CI/CD runs). Migration SQL itself lives with the backend at `backend/src/main/resources/db/migration/`.
- `grafana/` — (optional) prebuilt dashboards for raw snapshots once the data is flowing.

# Hosting options

The poller must run 24/7 to capture charging sessions live — Volvo doesn't expose history. Anywhere it isn't running, those sessions are lost. This doc compares realistic hosts for a single-user EX30 deployment.

## Quick comparison

| Option | One-time | Monthly | Timescale | Effort | Fit |
|---|---|---|---|---|---|
| Raspberry Pi 5 at home | ~€80 | ~€0.50 electricity | ✅ (your container) | Setup once + maintain hardware | Best if you like owning your infra |
| **Hetzner CX22 VPS** | €0 | ~€4 | ✅ (your container) | `docker compose up -d` + Caddy | **Best general-purpose recommendation** |
| Scaleway DEV1-S / Hostinger | €0 | ~€4–6 | ✅ | Same as Hetzner | Equivalent alternatives |
| GCP Cloud Run + Cloud SQL | €0 | ~$40–50 | ❌ Cloud SQL doesn't support Timescale | Higher (services, IAM, secrets) | Only if you're already deep in GCP |
| GCP e2-small VM (Compute Engine) | €0 | ~$13 | ✅ (your container) | Like Hetzner | More expensive than Hetzner for same thing |
| GCP free tier (e2-micro) | €0 | $0 | ✅ (your container) | Same | **US regions only**, not EU; latency to EU Volvo endpoint |
| Fly.io / Railway free | €0 | $0 | ❌ Free-tier Postgres auto-pauses | Medium | Polling silently dies during pause |

## Google Cloud, in detail

### Cloud Run + Cloud SQL

Cloud Run is GCP's serverless container platform. It's great for bursty HTTP services that scale to zero. **It's a poor fit here** for one reason: our backend isn't request-driven, it's a long-lived polling loop. To keep it alive 24/7 you must pin `min-instances=1`, which means it never scales to zero and you pay for steady CPU/memory.

| Component | Tier | Indicative cost |
|---|---|---|
| Cloud Run (1 always-on instance, 0.5 vCPU, 512 MB) | — | ~$15–25/mo |
| Cloud SQL Postgres (db-f1-micro, smallest) | Enterprise edition | ~$10–15/mo |
| Egress + network | — | ~$1–3/mo |
| **Total** | | **~$30–45/mo** |

**Timescale is unsupported** on Cloud SQL — Google only allows a curated extension list and Timescale isn't on it. Workaround: drop the `V2__timescale.sql` hypertable conversion and rely on the BRIN index already there. For one car at one poll/min, vanilla Postgres handles the snapshot volume comfortably (~50k rows/year).

### Compute Engine (a plain VM)

If you want GCP specifically, an `e2-small` Linux VM runs the existing `docker-compose.yml` unchanged — backend + Postgres-with-Timescale colocated in containers. ~$13/mo in `europe-west*`. That's the same shape as Hetzner but 3× the price; you pay for GCP's network and SRE.

### Free tier

GCP's Always-Free `e2-micro` (1 vCPU shared, 1 GB) is **US-region-only** (us-west1, us-central1, us-east1). Running it from Europe means every Volvo API call traverses the Atlantic — adds ~150 ms latency per request and counts against the 1 GB/mo egress free quota. Workable, but a poor experience.

### Verdict for this project

GCP isn't a natural fit for a single-user always-on poller. Cloud Run's serverless model fights our long-lived loop, Cloud SQL costs more than the whole rest of the stack, and Timescale isn't available there.

**If you specifically want GCP** (existing org account, free credits, learning), use `e2-small` + Docker — same shape as Hetzner, just more expensive. **Otherwise Hetzner CX22 (~€4/mo) is the cleanest answer**: existing `docker-compose.yml` works as-is, you stay on Timescale, low ops.

## What we need to change before deploying anywhere

1. **Token persistence** — wipes on every backend restart today; can't survive a host reboot. (Tracked.)
2. **Production redirect URIs** — register the public hostname's `/auth/callback` on the Volvo developer portal; set `VOLVO_REDIRECT_URI` and `WEB_APP_BASE_URL` to match.
3. **TLS** — Volvo's OAuth flow tolerates `http://localhost` but won't accept arbitrary `http://` redirect URIs in production. Caddy or Traefik fronting the backend gives free Let's Encrypt.
4. **Secrets out of the repo** — environment variables on the host or a `.env` mounted into the container; never committed.
5. **Postgres backup** — a cron'd `pg_dump` to S3-compatible storage (Backblaze B2, Hetzner Storage Box, etc.); at minimum a local copy on a second disk.
6. **Health endpoint** — single curl for "is the whole stack alive". Surfaces DB connectivity rather than letting the backend silently retry on a dead pool.

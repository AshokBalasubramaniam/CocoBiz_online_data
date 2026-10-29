# CocoBiz Backend

Rust (Axum) API for the CocoBiz app. MongoDB is the source of truth, Redis is
a read-through cache. Every account's data is isolated by username, and every
sensitive field (login password, user details, dealer/sales backup, reports)
is encrypted before it touches MongoDB.

## How data is protected

- **Passwords**: hashed with Argon2id (`src/crypto.rs`) — never stored or
  logged in plaintext, not even reversibly.
- **Everything else** (`user_details`, `full_details`, `reports`): encrypted
  with AES-256-GCM per record before being written to MongoDB. A random nonce
  is generated per encryption call. `ENCRYPTION_KEY` is the only thing that
  can decrypt this data — back it up outside of git/the database.
- **Per-user isolation**: every data route (`/profile`, `/sync`, `/reports`)
  reads the caller's username from their JWT, never from a request body or
  path parameter, so one account can never read or write another's records.
- **Auth**: JWT (HS256), issued on register/login, required as
  `Authorization: Bearer <token>` on every data route.

## Run with Docker (recommended)

```bash
cp .env.example .env
# fill in JWT_SECRET and ENCRYPTION_KEY, e.g.:
#   openssl rand -hex 32
docker compose up --build
```

This starts MongoDB, Redis, and the backend on `http://localhost:4000`.

## Run locally without Docker

Requires a local `mongod` and `redis-server`, plus the env vars in
`.env.example` (`cargo run` reads `.env` automatically).

```bash
cargo run
```

## API

| Method | Path                    | Auth | Description                              |
|--------|-------------------------|------|-------------------------------------------|
| GET    | `/health`               | no   | Liveness + Mongo/Redis connectivity       |
| POST   | `/api/v1/auth/register` | no   | Create account, returns a JWT             |
| POST   | `/api/v1/auth/login`    | no   | Returns a JWT                             |
| POST   | `/api/v1/auth/reset-password` | no | New password; needs username + registered phone |
| GET    | `/api/v1/profile`       | yes  | Get this account's business profile       |
| PUT    | `/api/v1/profile`       | yes  | Replace this account's business profile   |
| GET    | `/api/v1/sync`          | yes  | Get full backup (dealers + sales + profile) |
| PUT    | `/api/v1/sync`          | yes  | Replace full backup, recomputes reports   |
| GET    | `/api/v1/reports`       | yes  | Get the cached sales report summary       |

`sync`'s request/response shape mirrors the Android app's existing
`.cocobak` local backup format (`BackupManager.kt`), so the app's current
dealer/sales/profile JSON can be pushed here as-is.

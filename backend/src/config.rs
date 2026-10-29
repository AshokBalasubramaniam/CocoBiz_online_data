use std::env;

#[derive(Clone)]
pub struct Config {
    pub mongodb_uri: String,
    pub mongodb_db_name: String,
    pub redis_url: String,
    pub jwt_secret: String,
    pub jwt_ttl_seconds: i64,
    pub encryption_key: [u8; 32],
    pub cache_ttl_seconds: u64,
    pub port: u16,
}

impl Config {
    pub fn from_env() -> anyhow::Result<Self> {
        let jwt_secret = env::var("JWT_SECRET")
            .map_err(|_| anyhow::anyhow!("JWT_SECRET must be set (min 32 characters)"))?;
        if jwt_secret.len() < 32 {
            anyhow::bail!("JWT_SECRET must be at least 32 characters long");
        }

        let encryption_key_hex = env::var("ENCRYPTION_KEY").map_err(|_| {
            anyhow::anyhow!(
                "ENCRYPTION_KEY must be set to a 64-character hex string (32 bytes / AES-256 key)"
            )
        })?;
        let key_bytes = hex_decode(&encryption_key_hex)?;
        if key_bytes.len() != 32 {
            anyhow::bail!("ENCRYPTION_KEY must decode to exactly 32 bytes (64 hex chars)");
        }
        let mut encryption_key = [0u8; 32];
        encryption_key.copy_from_slice(&key_bytes);

        Ok(Self {
            mongodb_uri: env::var("MONGODB_URI")
                .unwrap_or_else(|_| "mongodb://localhost:27017".to_string()),
            mongodb_db_name: env::var("MONGODB_DB_NAME").unwrap_or_else(|_| "cocobiz".to_string()),
            redis_url: env::var("REDIS_URL")
                .unwrap_or_else(|_| "redis://127.0.0.1:6379".to_string()),
            jwt_secret,
            jwt_ttl_seconds: env::var("JWT_TTL_SECONDS")
                .ok()
                .and_then(|v| v.parse().ok())
                .unwrap_or(60 * 60 * 24 * 7),
            encryption_key,
            cache_ttl_seconds: env::var("CACHE_TTL_SECONDS")
                .ok()
                .and_then(|v| v.parse().ok())
                .unwrap_or(300),
            port: env::var("PORT")
                .ok()
                .and_then(|v| v.parse().ok())
                .unwrap_or(4000),
        })
    }
}

fn hex_decode(s: &str) -> anyhow::Result<Vec<u8>> {
    let s = s.trim();
    if s.len() % 2 != 0 {
        anyhow::bail!("ENCRYPTION_KEY hex string must have an even length");
    }
    (0..s.len())
        .step_by(2)
        .map(|i| {
            u8::from_str_radix(&s[i..i + 2], 16)
                .map_err(|_| anyhow::anyhow!("ENCRYPTION_KEY contains invalid hex characters"))
        })
        .collect()
}

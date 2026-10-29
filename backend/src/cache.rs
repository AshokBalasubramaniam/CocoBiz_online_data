use redis::AsyncCommands;
use redis::aio::ConnectionManager;
use serde::Serialize;
use serde::de::DeserializeOwned;

/// Thin async Redis wrapper. Every cached value here is a decrypted, already
/// user-scoped JSON payload (profile / reports / full backup) with a short
/// TTL — Mongo stays the source of truth, Redis just saves a decrypt round
/// trip for repeat reads within the TTL window.
#[derive(Clone)]
pub struct Cache {
    conn: ConnectionManager,
    ttl_seconds: u64,
}

impl Cache {
    pub async fn connect(url: &str, ttl_seconds: u64) -> redis::RedisResult<Self> {
        let client = redis::Client::open(url)?;
        let conn = client.get_connection_manager().await?;
        Ok(Self { conn, ttl_seconds })
    }

    pub async fn get_json<T: DeserializeOwned>(&self, key: &str) -> Option<T> {
        let mut conn = self.conn.clone();
        let raw: Option<String> = conn.get(key).await.ok()?;
        raw.and_then(|s| serde_json::from_str(&s).ok())
    }

    pub async fn set_json<T: Serialize>(&self, key: &str, value: &T) {
        if let Ok(s) = serde_json::to_string(value) {
            let mut conn = self.conn.clone();
            let _: Result<(), _> = conn.set_ex(key, s, self.ttl_seconds).await;
        }
    }

    pub async fn ping(&self) -> redis::RedisResult<()> {
        let mut conn = self.conn.clone();
        redis::cmd("PING").query_async::<String>(&mut conn).await?;
        Ok(())
    }
}

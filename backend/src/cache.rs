use redis::AsyncCommands;
use redis::aio::ConnectionManager;
use serde::Serialize;
use serde::de::DeserializeOwned;

/// Thin async Redis wrapper. Every cached value here is a decrypted, already
/// user-scoped JSON payload (profile / reports / full backup) with a short
/// TTL — Mongo stays the source of truth, Redis just saves a decrypt round
/// trip for repeat reads within the TTL window.
///
/// Redis is optional: without `REDIS_URL` (or if it can't be reached at
/// startup) every read goes straight to MongoDB and writes are skipped.
#[derive(Clone)]
pub struct Cache {
    conn: Option<ConnectionManager>,
    ttl_seconds: u64,
}

impl Cache {
    pub async fn connect(url: Option<&str>, ttl_seconds: u64) -> Self {
        let Some(url) = url else {
            tracing::warn!("REDIS_URL not set — running without cache (MongoDB only)");
            return Self { conn: None, ttl_seconds };
        };
        let conn = match redis::Client::open(url) {
            Ok(client) => client.get_connection_manager().await,
            Err(e) => Err(e),
        };
        match conn {
            Ok(conn) => Self { conn: Some(conn), ttl_seconds },
            Err(e) => {
                tracing::warn!(error = %e, "could not connect to Redis — running without cache (MongoDB only)");
                Self { conn: None, ttl_seconds }
            }
        }
    }

    pub fn is_enabled(&self) -> bool {
        self.conn.is_some()
    }

    pub async fn get_json<T: DeserializeOwned>(&self, key: &str) -> Option<T> {
        let mut conn = self.conn.clone()?;
        let raw: Option<String> = conn.get(key).await.ok()?;
        raw.and_then(|s| serde_json::from_str(&s).ok())
    }

    pub async fn set_json<T: Serialize>(&self, key: &str, value: &T) {
        let Some(mut conn) = self.conn.clone() else {
            return;
        };
        if let Ok(s) = serde_json::to_string(value) {
            let _: Result<(), _> = conn.set_ex(key, s, self.ttl_seconds).await;
        }
    }

    pub async fn delete(&self, keys: &[String]) {
        let Some(mut conn) = self.conn.clone() else {
            return;
        };
        let _: Result<(), _> = conn.del(keys).await;
    }

    pub async fn ping(&self) -> redis::RedisResult<()> {
        let Some(mut conn) = self.conn.clone() else {
            return Ok(());
        };
        redis::cmd("PING").query_async::<String>(&mut conn).await?;
        Ok(())
    }
}

pub fn bundle_key(user_id: &mongodb::bson::oid::ObjectId) -> String {
    format!("cocobiz:full:{}", user_id.to_hex())
}

pub fn reports_key(user_id: &mongodb::bson::oid::ObjectId) -> String {
    format!("cocobiz:reports:{}", user_id.to_hex())
}

/// Drops every cached view of this user's data; call after any write.
pub async fn invalidate_user(cache: &Cache, user_id: &mongodb::bson::oid::ObjectId) {
    cache.delete(&[bundle_key(user_id), reports_key(user_id)]).await;
}

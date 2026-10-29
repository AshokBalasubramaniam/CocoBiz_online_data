mod cache;
mod config;
mod crypto;
mod db;
mod error;
mod extractors;
mod handlers;
mod jwt;
mod models;
mod routes;
mod state;

use std::sync::Arc;

use crypto::Encryptor;
use state::AppState;

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    dotenvy::dotenv().ok();

    tracing_subscriber::fmt()
        .with_env_filter(
            tracing_subscriber::EnvFilter::try_from_default_env()
                .unwrap_or_else(|_| tracing_subscriber::EnvFilter::new("info")),
        )
        .init();

    let config = config::Config::from_env()?;
    let port = config.port;

    tracing::info!("connecting to MongoDB...");
    let db = db::connect(&config.mongodb_uri, &config.mongodb_db_name).await?;
    db::ensure_indexes(&db).await?;

    let cache = cache::Cache::connect(config.redis_url.as_deref(), config.cache_ttl_seconds).await;

    let encryptor = Encryptor::new(&config.encryption_key);

    let state = AppState {
        db,
        cache,
        config: Arc::new(config),
        encryptor: Arc::new(encryptor),
    };

    let app = routes::build(state);

    let listener = tokio::net::TcpListener::bind(("0.0.0.0", port)).await?;
    tracing::info!("cocobiz backend listening on 0.0.0.0:{port}");
    axum::serve(listener, app).await?;

    Ok(())
}

use axum::Json;
use axum::extract::State;
use axum::http::StatusCode;
use mongodb::bson::doc;
use serde_json::json;

use crate::state::AppState;

pub async fn health(State(state): State<AppState>) -> (StatusCode, Json<serde_json::Value>) {
    let mongo_ok = state
        .db
        .run_command(doc! { "ping": 1 })
        .await
        .is_ok();
    let redis_ok = state.cache.ping().await.is_ok();

    let status = if mongo_ok && redis_ok {
        StatusCode::OK
    } else {
        StatusCode::SERVICE_UNAVAILABLE
    };

    (
        status,
        Json(json!({
            "status": if status == StatusCode::OK { "ok" } else { "degraded" },
            "mongodb": mongo_ok,
            "redis": redis_ok,
            "cache_enabled": state.cache.is_enabled(),
        })),
    )
}

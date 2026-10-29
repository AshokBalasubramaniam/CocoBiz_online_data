use axum::Json;
use axum::extract::State;

use crate::cache;
use crate::error::AppError;
use crate::extractors::AuthUser;
use crate::models::FullBackupDto;
use crate::state::AppState;
use crate::store;

/// Full data pull — dealers, sales, profile and settings for the caller,
/// read from the per-user collections.
pub async fn get_sync(
    State(state): State<AppState>,
    auth: AuthUser,
) -> Result<Json<FullBackupDto>, AppError> {
    let key = cache::bundle_key(&auth.user_id);
    if let Some(cached) = state.cache.get_json::<FullBackupDto>(&key).await {
        return Ok(Json(cached));
    }
    let bundle = store::load_bundle(&state.db, auth.user_id).await?;
    state.cache.set_json(&key, &bundle).await;
    Ok(Json(bundle))
}

/// Full data push — the caller's rows are made to match the bundle; only new
/// or changed rows are written, and rows missing from it are deleted.
pub async fn put_sync(
    State(state): State<AppState>,
    auth: AuthUser,
    Json(body): Json<FullBackupDto>,
) -> Result<Json<FullBackupDto>, AppError> {
    store::save_bundle(&state.db, auth.user_id, body).await?;
    cache::invalidate_user(&state.cache, &auth.user_id).await;
    Ok(Json(store::load_bundle(&state.db, auth.user_id).await?))
}

use axum::Json;
use axum::extract::State;
use chrono::Utc;

use crate::cache;
use crate::error::AppError;
use crate::extractors::AuthUser;
use crate::models::{AppSettingsDto, UserProfileDto};
use crate::state::AppState;
use crate::store;

pub async fn get_profile(
    State(state): State<AppState>,
    auth: AuthUser,
) -> Result<Json<UserProfileDto>, AppError> {
    store::get_profile(&state.db, auth.user_id)
        .await?
        .map(Json)
        .ok_or_else(|| AppError::NotFound("no profile found for this account".to_string()))
}

pub async fn put_profile(
    State(state): State<AppState>,
    auth: AuthUser,
    Json(mut body): Json<UserProfileDto>,
) -> Result<Json<UserProfileDto>, AppError> {
    body.updated_at = Utc::now().timestamp_millis();
    store::put_profile(&state.db, auth.user_id, body.clone()).await?;
    cache::invalidate_user(&state.cache, &auth.user_id).await;
    Ok(Json(body))
}

pub async fn get_settings(
    State(state): State<AppState>,
    auth: AuthUser,
) -> Result<Json<AppSettingsDto>, AppError> {
    store::get_settings(&state.db, auth.user_id)
        .await?
        .map(Json)
        .ok_or_else(|| AppError::NotFound("no settings saved for this account".to_string()))
}

pub async fn put_settings(
    State(state): State<AppState>,
    auth: AuthUser,
    Json(body): Json<AppSettingsDto>,
) -> Result<Json<AppSettingsDto>, AppError> {
    store::put_settings(&state.db, auth.user_id, body.clone()).await?;
    cache::invalidate_user(&state.cache, &auth.user_id).await;
    Ok(Json(body))
}

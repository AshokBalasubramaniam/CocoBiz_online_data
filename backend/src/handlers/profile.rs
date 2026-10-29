use axum::Json;
use axum::extract::State;
use chrono::Utc;
use mongodb::bson::doc;

use crate::db;
use crate::error::AppError;
use crate::extractors::AuthUser;
use crate::models::UserProfileDto;
use crate::state::AppState;

fn cache_key(username_lower: &str) -> String {
    format!("cocobiz:profile:{username_lower}")
}

pub async fn get_profile(
    State(state): State<AppState>,
    auth: AuthUser,
) -> Result<Json<UserProfileDto>, AppError> {
    let key = cache_key(&auth.username_lower);
    if let Some(cached) = state.cache.get_json::<UserProfileDto>(&key).await {
        return Ok(Json(cached));
    }

    let doc = db::user_data_collection(&state.db)
        .find_one(doc! { "username_lower": &auth.username_lower })
        .await?
        .ok_or_else(|| AppError::NotFound("no profile found for this account".to_string()))?;

    let profile = match doc.enc_user_details {
        Some(blob) => state.encryptor.decrypt_json::<UserProfileDto>(&blob)?,
        None => UserProfileDto::default(),
    };

    state.cache.set_json(&key, &profile).await;
    Ok(Json(profile))
}

pub async fn put_profile(
    State(state): State<AppState>,
    auth: AuthUser,
    Json(mut body): Json<UserProfileDto>,
) -> Result<Json<UserProfileDto>, AppError> {
    let now = Utc::now();
    body.updated_at = now.timestamp_millis();

    let blob = state.encryptor.encrypt_json(&body)?;
    let blob_bson = mongodb::bson::to_bson(&blob)?;

    db::user_data_collection(&state.db)
        .update_one(
            doc! { "username_lower": &auth.username_lower },
            doc! { "$set": { "enc_user_details": blob_bson, "updated_at": now } },
        )
        .upsert(true)
        .await?;

    state.cache.set_json(&cache_key(&auth.username_lower), &body).await;
    Ok(Json(body))
}

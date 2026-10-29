use axum::Json;
use axum::extract::State;
use chrono::Utc;
use mongodb::bson::doc;

use crate::db;
use crate::error::AppError;
use crate::extractors::AuthUser;
use crate::models::{FullBackupDto, ReportsSummaryDto};
use crate::state::AppState;

fn full_cache_key(username_lower: &str) -> String {
    format!("cocobiz:full:{username_lower}")
}

fn reports_cache_key(username_lower: &str) -> String {
    format!("cocobiz:reports:{username_lower}")
}

/// Full data pull — the cloud equivalent of the app's local `.cocobak`
/// restore: dealers, sales, and profile in one encrypted-at-rest bundle.
pub async fn get_sync(
    State(state): State<AppState>,
    auth: AuthUser,
) -> Result<Json<FullBackupDto>, AppError> {
    let key = full_cache_key(&auth.username_lower);
    if let Some(cached) = state.cache.get_json::<FullBackupDto>(&key).await {
        return Ok(Json(cached));
    }

    let doc = db::user_data_collection(&state.db)
        .find_one(doc! { "username_lower": &auth.username_lower })
        .await?
        .ok_or_else(|| AppError::NotFound("no data found for this account".to_string()))?;

    let bundle = match doc.enc_full_details {
        Some(blob) => state.encryptor.decrypt_json::<FullBackupDto>(&blob)?,
        None => FullBackupDto::default(),
    };

    state.cache.set_json(&key, &bundle).await;
    Ok(Json(bundle))
}

/// Full data push — replaces this account's stored dealers/sales/profile
/// bundle and recomputes the reports summary from the new sales data.
pub async fn put_sync(
    State(state): State<AppState>,
    auth: AuthUser,
    Json(body): Json<FullBackupDto>,
) -> Result<Json<FullBackupDto>, AppError> {
    let now = Utc::now();

    let full_blob = state.encryptor.encrypt_json(&body)?;
    let reports = ReportsSummaryDto::compute(&body.sales);
    let reports_blob = state.encryptor.encrypt_json(&reports)?;

    db::user_data_collection(&state.db)
        .update_one(
            doc! { "username_lower": &auth.username_lower },
            doc! {
                "$set": {
                    "enc_full_details": mongodb::bson::to_bson(&full_blob)?,
                    "enc_reports": mongodb::bson::to_bson(&reports_blob)?,
                    "updated_at": now,
                }
            },
        )
        .upsert(true)
        .await?;

    state.cache.set_json(&full_cache_key(&auth.username_lower), &body).await;
    state
        .cache
        .set_json(&reports_cache_key(&auth.username_lower), &reports)
        .await;

    Ok(Json(body))
}

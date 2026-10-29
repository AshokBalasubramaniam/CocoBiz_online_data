use axum::Json;
use axum::extract::State;
use mongodb::bson::doc;

use crate::db;
use crate::error::AppError;
use crate::extractors::AuthUser;
use crate::models::ReportsSummaryDto;
use crate::state::AppState;

fn cache_key(username_lower: &str) -> String {
    format!("cocobiz:reports:{username_lower}")
}

pub async fn get_reports(
    State(state): State<AppState>,
    auth: AuthUser,
) -> Result<Json<ReportsSummaryDto>, AppError> {
    let key = cache_key(&auth.username_lower);
    if let Some(cached) = state.cache.get_json::<ReportsSummaryDto>(&key).await {
        return Ok(Json(cached));
    }

    let doc = db::user_data_collection(&state.db)
        .find_one(doc! { "username_lower": &auth.username_lower })
        .await?
        .ok_or_else(|| AppError::NotFound("no data found for this account".to_string()))?;

    let reports = match doc.enc_reports {
        Some(blob) => state.encryptor.decrypt_json::<ReportsSummaryDto>(&blob)?,
        None => ReportsSummaryDto::default(),
    };

    state.cache.set_json(&key, &reports).await;
    Ok(Json(reports))
}

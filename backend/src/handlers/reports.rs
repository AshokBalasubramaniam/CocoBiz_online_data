use axum::Json;
use axum::extract::State;

use crate::cache;
use crate::error::AppError;
use crate::extractors::AuthUser;
use crate::models::ReportsSummaryDto;
use crate::state::AppState;
use crate::store;

/// Sales summary computed from the caller's rows in the `sales` collection.
pub async fn get_reports(
    State(state): State<AppState>,
    auth: AuthUser,
) -> Result<Json<ReportsSummaryDto>, AppError> {
    let key = cache::reports_key(&auth.user_id);
    if let Some(cached) = state.cache.get_json::<ReportsSummaryDto>(&key).await {
        return Ok(Json(cached));
    }
    let sales = store::list_sales(&state.db, auth.user_id, None).await?;
    let reports = ReportsSummaryDto::compute(&sales);
    state.cache.set_json(&key, &reports).await;
    Ok(Json(reports))
}

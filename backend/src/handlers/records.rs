//! Row-level endpoints for the `dealers` and `sales` collections.

use axum::Json;
use axum::extract::{Path, Query, State};
use axum::http::StatusCode;
use serde::Deserialize;

use crate::cache;
use crate::error::AppError;
use crate::extractors::AuthUser;
use crate::models::{DealerDto, SalesEntryDto};
use crate::state::AppState;
use crate::store;

pub async fn list_dealers(
    State(state): State<AppState>,
    auth: AuthUser,
) -> Result<Json<Vec<DealerDto>>, AppError> {
    Ok(Json(store::list_dealers(&state.db, auth.user_id).await?))
}

pub async fn put_dealer(
    State(state): State<AppState>,
    auth: AuthUser,
    Path(id): Path<i64>,
    Json(mut body): Json<DealerDto>,
) -> Result<Json<DealerDto>, AppError> {
    body.id = id;
    store::put_dealer(&state.db, auth.user_id, body.clone()).await?;
    cache::invalidate_user(&state.cache, &auth.user_id).await;
    Ok(Json(body))
}

pub async fn delete_dealer(
    State(state): State<AppState>,
    auth: AuthUser,
    Path(id): Path<i64>,
) -> Result<StatusCode, AppError> {
    let deleted = store::delete_dealer(&state.db, auth.user_id, id).await?;
    cache::invalidate_user(&state.cache, &auth.user_id).await;
    if deleted {
        Ok(StatusCode::NO_CONTENT)
    } else {
        Err(AppError::NotFound("dealer not found".to_string()))
    }
}

#[derive(Debug, Deserialize)]
pub struct SalesQuery {
    pub status: Option<String>,
}

pub async fn list_sales(
    State(state): State<AppState>,
    auth: AuthUser,
    Query(query): Query<SalesQuery>,
) -> Result<Json<Vec<SalesEntryDto>>, AppError> {
    Ok(Json(
        store::list_sales(&state.db, auth.user_id, query.status.as_deref()).await?,
    ))
}

pub async fn put_sale(
    State(state): State<AppState>,
    auth: AuthUser,
    Path(id): Path<i64>,
    Json(mut body): Json<SalesEntryDto>,
) -> Result<Json<SalesEntryDto>, AppError> {
    body.id = id;
    store::put_sale(&state.db, auth.user_id, body.clone()).await?;
    cache::invalidate_user(&state.cache, &auth.user_id).await;
    Ok(Json(body))
}

pub async fn delete_sale(
    State(state): State<AppState>,
    auth: AuthUser,
    Path(id): Path<i64>,
) -> Result<StatusCode, AppError> {
    let deleted = store::delete_sale(&state.db, auth.user_id, id).await?;
    cache::invalidate_user(&state.cache, &auth.user_id).await;
    if deleted {
        Ok(StatusCode::NO_CONTENT)
    } else {
        Err(AppError::NotFound("sale not found".to_string()))
    }
}

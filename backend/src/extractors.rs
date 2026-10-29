use axum::extract::FromRequestParts;
use axum::http::request::Parts;
use axum::http::header::AUTHORIZATION;
use mongodb::bson::doc;
use mongodb::bson::oid::ObjectId;

use crate::db;
use crate::error::AppError;
use crate::jwt;
use crate::migrate;
use crate::models::CURRENT_DATA_VERSION;
use crate::state::AppState;

/// The authenticated caller, resolved from a `Authorization: Bearer <jwt>`
/// header. Every per-user route takes this instead of a username path
/// parameter, so a request can only ever read or write the data belonging
/// to whoever holds the token — there is no way to pass someone else's
/// username in and access their records.
pub struct AuthUser {
    /// `users._id` — the owner key stored on every business-data row.
    pub user_id: ObjectId,
}

impl FromRequestParts<AppState> for AuthUser {
    type Rejection = AppError;

    async fn from_request_parts(
        parts: &mut Parts,
        state: &AppState,
    ) -> Result<Self, Self::Rejection> {
        let header_value = parts
            .headers
            .get(AUTHORIZATION)
            .and_then(|v| v.to_str().ok())
            .ok_or_else(|| AppError::Auth("missing Authorization header".to_string()))?;

        let token = header_value
            .strip_prefix("Bearer ")
            .ok_or_else(|| AppError::Auth("Authorization header must be a Bearer token".to_string()))?;

        let claims = jwt::verify_token(token, &state.config.jwt_secret)?;

        let user = db::users_collection(&state.db)
            .find_one(doc! { "username_lower": &claims.sub })
            .await?
            .ok_or_else(|| AppError::Auth("account no longer exists".to_string()))?;
        let user_id = user
            .id
            .ok_or_else(|| AppError::Internal("user document has no _id".to_string()))?;

        if user.data_version < CURRENT_DATA_VERSION {
            migrate::migrate_legacy_user(state, &claims.sub, user_id).await?;
        }

        Ok(AuthUser { user_id })
    }
}

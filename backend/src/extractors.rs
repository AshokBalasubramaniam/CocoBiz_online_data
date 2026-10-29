use axum::extract::FromRequestParts;
use axum::http::request::Parts;
use axum::http::header::AUTHORIZATION;

use crate::error::AppError;
use crate::jwt;
use crate::state::AppState;

/// The authenticated caller, resolved from a `Authorization: Bearer <jwt>`
/// header. Every per-user route takes this instead of a username path
/// parameter, so a request can only ever read or write the data belonging
/// to whoever holds the token — there is no way to pass someone else's
/// username in and access their records.
pub struct AuthUser {
    pub username_lower: String,
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

        Ok(AuthUser {
            username_lower: claims.sub,
        })
    }
}

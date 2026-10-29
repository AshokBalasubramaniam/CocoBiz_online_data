use chrono::Utc;
use jsonwebtoken::{Algorithm, DecodingKey, EncodingKey, Header, Validation, decode, encode};
use serde::{Deserialize, Serialize};

use crate::error::AppError;

#[derive(Debug, Serialize, Deserialize)]
pub struct Claims {
    /// Username (lowercased) this token was issued for — every data route
    /// scopes its MongoDB/Redis lookups to this value, so one user can never
    /// read or write another user's data even if they guess an id.
    pub sub: String,
    pub iat: i64,
    pub exp: i64,
}

pub fn create_token(username_lower: &str, secret: &str, ttl_seconds: i64) -> Result<String, AppError> {
    let now = Utc::now().timestamp();
    let claims = Claims {
        sub: username_lower.to_string(),
        iat: now,
        exp: now + ttl_seconds,
    };
    encode(&Header::default(), &claims, &EncodingKey::from_secret(secret.as_bytes()))
        .map_err(AppError::from)
}

pub fn verify_token(token: &str, secret: &str) -> Result<Claims, AppError> {
    let data = decode::<Claims>(
        token,
        &DecodingKey::from_secret(secret.as_bytes()),
        &Validation::new(Algorithm::HS256),
    )?;
    Ok(data.claims)
}

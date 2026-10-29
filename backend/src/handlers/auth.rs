use axum::Json;
use axum::extract::State;
use chrono::Utc;
use mongodb::bson::doc;

use crate::crypto;
use crate::db;
use crate::error::AppError;
use crate::jwt;
use crate::models::{
    AuthResponse, LoginRequest, RegisterRequest, ResetPasswordRequest, UserDataDoc, UserDoc,
    UserProfileDto,
};
use crate::state::AppState;

pub async fn register(
    State(state): State<AppState>,
    Json(req): Json<RegisterRequest>,
) -> Result<Json<AuthResponse>, AppError> {
    let username = req.username.trim().to_string();
    if username.is_empty() {
        return Err(AppError::Validation("username is required".to_string()));
    }
    if req.password.len() < 6 {
        return Err(AppError::Validation(
            "password must be at least 6 characters".to_string(),
        ));
    }
    let username_lower = username.to_lowercase();

    let users = db::users_collection(&state.db);
    if users
        .find_one(doc! { "username_lower": &username_lower })
        .await?
        .is_some()
    {
        return Err(AppError::Conflict("username is already taken".to_string()));
    }

    let password_hash = crypto::hash_password(&req.password)?;
    let now = Utc::now();

    let user_doc = UserDoc {
        id: None,
        username: username.clone(),
        username_lower: username_lower.clone(),
        password_hash,
        email: req.email.clone(),
        phone: req.phone.clone(),
        created_at: now,
    };
    users.insert_one(user_doc).await?;

    let profile = UserProfileDto {
        business_name: req.business_name,
        owner_name: req.owner_name,
        phone: req.phone,
        email: req.email,
        created_at: now.timestamp_millis(),
        updated_at: now.timestamp_millis(),
        ..Default::default()
    };
    let enc_user_details = state.encryptor.encrypt_json(&profile)?;

    let user_data_doc = UserDataDoc {
        id: None,
        username_lower: username_lower.clone(),
        enc_user_details: Some(enc_user_details),
        enc_full_details: None,
        enc_reports: None,
        updated_at: now,
    };
    db::user_data_collection(&state.db)
        .insert_one(user_data_doc)
        .await?;

    let token = jwt::create_token(&username_lower, &state.config.jwt_secret, state.config.jwt_ttl_seconds)?;
    Ok(Json(AuthResponse {
        token,
        token_type: "Bearer".to_string(),
        expires_in: state.config.jwt_ttl_seconds,
        username,
    }))
}

pub async fn login(
    State(state): State<AppState>,
    Json(req): Json<LoginRequest>,
) -> Result<Json<AuthResponse>, AppError> {
    let username_lower = req.username.trim().to_lowercase();
    let invalid = || AppError::Auth("invalid username or password".to_string());

    let user = db::users_collection(&state.db)
        .find_one(doc! { "username_lower": &username_lower })
        .await?
        .ok_or_else(invalid)?;

    if !crypto::verify_password(&req.password, &user.password_hash)? {
        return Err(invalid());
    }

    let token = jwt::create_token(&username_lower, &state.config.jwt_secret, state.config.jwt_ttl_seconds)?;
    Ok(Json(AuthResponse {
        token,
        token_type: "Bearer".to_string(),
        expires_in: state.config.jwt_ttl_seconds,
        username: user.username,
    }))
}

pub async fn reset_password(
    State(state): State<AppState>,
    Json(req): Json<ResetPasswordRequest>,
) -> Result<Json<serde_json::Value>, AppError> {
    if req.new_password.len() < 6 {
        return Err(AppError::Validation(
            "password must be at least 6 characters".to_string(),
        ));
    }
    let username_lower = req.username.trim().to_lowercase();
    let mismatch = || AppError::Auth("username and phone number do not match".to_string());

    let users = db::users_collection(&state.db);
    let user = users
        .find_one(doc! { "username_lower": &username_lower })
        .await?
        .ok_or_else(mismatch)?;

    if digits_only(&user.phone).is_empty() || digits_only(&user.phone) != digits_only(&req.phone) {
        return Err(mismatch());
    }

    let password_hash = crypto::hash_password(&req.new_password)?;
    users
        .update_one(
            doc! { "username_lower": &username_lower },
            doc! { "$set": { "password_hash": password_hash } },
        )
        .await?;

    Ok(Json(serde_json::json!({ "ok": true })))
}

fn digits_only(s: &str) -> String {
    s.chars().filter(|c| c.is_ascii_digit()).collect()
}

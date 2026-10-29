use chrono::{DateTime, Utc};
use mongodb::bson::oid::ObjectId;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;

use crate::crypto::EncryptedBlob;

// ---------- MongoDB documents ----------

/// One document per account. Holds only credentials — never plaintext
/// business data — so a leak of this collection alone exposes no user data.
#[derive(Debug, Serialize, Deserialize)]
pub struct UserDoc {
    #[serde(rename = "_id", skip_serializing_if = "Option::is_none")]
    pub id: Option<ObjectId>,
    pub username: String,
    pub username_lower: String,
    pub password_hash: String,
    pub email: String,
    pub phone: String,
    #[serde(with = "mongodb::bson::serde_helpers::chrono_datetime_as_bson_datetime")]
    pub created_at: DateTime<Utc>,
}

/// One document per account, keyed by `username_lower`. Every field that can
/// hold business data is an [`EncryptedBlob`] (AES-256-GCM) — MongoDB never
/// sees plaintext dealer names, sales figures, or contact details.
#[derive(Debug, Serialize, Deserialize)]
pub struct UserDataDoc {
    #[serde(rename = "_id", skip_serializing_if = "Option::is_none")]
    pub id: Option<ObjectId>,
    pub username_lower: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enc_user_details: Option<EncryptedBlob>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enc_full_details: Option<EncryptedBlob>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enc_reports: Option<EncryptedBlob>,
    #[serde(with = "mongodb::bson::serde_helpers::chrono_datetime_as_bson_datetime")]
    pub updated_at: DateTime<Utc>,
}

// ---------- Auth DTOs ----------

#[derive(Debug, Deserialize)]
pub struct RegisterRequest {
    pub username: String,
    pub password: String,
    #[serde(default)]
    pub email: String,
    #[serde(default)]
    pub phone: String,
    #[serde(default)]
    pub business_name: String,
    #[serde(default)]
    pub owner_name: String,
}

#[derive(Debug, Deserialize)]
pub struct LoginRequest {
    pub username: String,
    pub password: String,
}

/// Password reset without a session. The caller must also supply the phone
/// number the account was registered with, so a username alone is not enough
/// to take over an account.
#[derive(Debug, Deserialize)]
pub struct ResetPasswordRequest {
    pub username: String,
    pub phone: String,
    pub new_password: String,
}

#[derive(Debug, Serialize)]
pub struct AuthResponse {
    pub token: String,
    pub token_type: String,
    pub expires_in: i64,
    pub username: String,
}

// ---------- Business data DTOs (plaintext shape; encrypted once at rest) ----------
// These mirror the Android app's Room entities (UserProfileEntity, DealerEntity,
// SalesEntryEntity) so the mobile app's existing JSON backup format maps 1:1
// onto this API.

#[derive(Debug, Default, Clone, Serialize, Deserialize)]
pub struct UserProfileDto {
    #[serde(default)]
    pub business_name: String,
    #[serde(default)]
    pub owner_name: String,
    #[serde(default)]
    pub phone: String,
    #[serde(default)]
    pub alternate_phone: String,
    #[serde(default)]
    pub email: String,
    #[serde(default)]
    pub address: String,
    #[serde(default)]
    pub city: String,
    #[serde(default)]
    pub state: String,
    #[serde(default)]
    pub pincode: String,
    #[serde(default)]
    pub gst_number: String,
    #[serde(default)]
    pub created_at: i64,
    #[serde(default)]
    pub updated_at: i64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DealerDto {
    pub id: i64,
    pub dealer_name: String,
    pub place: String,
    pub phone: String,
    #[serde(default)]
    pub alternate_phone: String,
    #[serde(default)]
    pub email: String,
    #[serde(default)]
    pub address: String,
    #[serde(default)]
    pub city: String,
    #[serde(default)]
    pub state: String,
    #[serde(default)]
    pub pincode: String,
    #[serde(default)]
    pub notes: String,
    #[serde(default = "default_true")]
    pub is_active: bool,
    pub created_at: i64,
    pub updated_at: i64,
}

fn default_true() -> bool {
    true
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SalesEntryDto {
    pub id: i64,
    pub dealer_id: i64,
    pub dealer_name: String,
    pub dealer_place: String,
    pub sales_date: i64,
    pub next_sales_date: i64,
    pub quantity: f64,
    pub rate: f64,
    #[serde(default)]
    pub market_rate: f64,
    pub total_amount: f64,
    pub coconut_type: String,
    #[serde(default = "default_cycle_days")]
    pub cycle_days: i32,
    #[serde(default = "default_status")]
    pub status: String,
    #[serde(default)]
    pub notes: String,
    pub created_at: i64,
    pub updated_at: i64,
}

fn default_cycle_days() -> i32 {
    60
}

fn default_status() -> String {
    "ACTIVE".to_string()
}

/// Mirrors the app's SettingsEntity plus the account's reminder preferences,
/// so an account's preferences follow it to a new device.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AppSettingsDto {
    #[serde(default = "default_reminder_days")]
    pub reminder_days: i32,
    #[serde(default = "default_true")]
    pub notification_enabled: bool,
    #[serde(default)]
    pub email_enabled: bool,
    #[serde(default)]
    pub sender_email: String,
    #[serde(default = "default_dark_mode")]
    pub dark_mode: String,
    #[serde(default)]
    pub backup_enabled: bool,
    #[serde(default = "default_cycle_days")]
    pub default_cycle_days: i32,
    #[serde(default)]
    pub edit_completed_enabled: bool,
    #[serde(default = "default_reminder_channel")]
    pub reminder_channel: String,
    #[serde(default = "default_reminder_frequency")]
    pub reminder_frequency: String,
}

fn default_reminder_days() -> i32 {
    5
}

fn default_dark_mode() -> String {
    "SYSTEM".to_string()
}

fn default_reminder_channel() -> String {
    "EMAIL".to_string()
}

fn default_reminder_frequency() -> String {
    "DAILY".to_string()
}

#[derive(Debug, Default, Clone, Serialize, Deserialize)]
pub struct FullBackupDto {
    #[serde(default)]
    pub dealers: Vec<DealerDto>,
    #[serde(default)]
    pub sales: Vec<SalesEntryDto>,
    #[serde(default)]
    pub profile: Option<UserProfileDto>,
    #[serde(default)]
    pub settings: Option<AppSettingsDto>,
}

#[derive(Debug, Default, Clone, Serialize, Deserialize)]
pub struct ReportsSummaryDto {
    pub total_revenue: f64,
    pub total_quantity: f64,
    pub total_sales_count: i64,
    pub average_order_value: f64,
    pub revenue_by_coconut_type: HashMap<String, f64>,
    pub generated_at: i64,
}

impl ReportsSummaryDto {
    pub fn compute(sales: &[SalesEntryDto]) -> Self {
        let total_revenue: f64 = sales.iter().map(|s| s.total_amount).sum();
        let total_quantity: f64 = sales.iter().map(|s| s.quantity).sum();
        let total_sales_count = sales.len() as i64;
        let average_order_value = if total_sales_count > 0 {
            total_revenue / total_sales_count as f64
        } else {
            0.0
        };
        let mut revenue_by_coconut_type: HashMap<String, f64> = HashMap::new();
        for s in sales {
            *revenue_by_coconut_type
                .entry(s.coconut_type.clone())
                .or_insert(0.0) += s.total_amount;
        }
        Self {
            total_revenue,
            total_quantity,
            total_sales_count,
            average_order_value,
            revenue_by_coconut_type,
            generated_at: Utc::now().timestamp_millis(),
        }
    }
}

use mongodb::bson::doc;
use mongodb::bson::oid::ObjectId;

use crate::db;
use crate::error::AppError;
use crate::models::{CURRENT_DATA_VERSION, FullBackupDto, UserProfileDto};
use crate::state::AppState;
use crate::store;

/// Moves an account from the legacy single encrypted `user_data` blob into
/// the per-user collections. Runs once, on that account's first request
/// after the upgrade. The old `user_data` document is left in place as a
/// backup.
pub async fn migrate_legacy_user(state: &AppState, username_lower: &str, user_id: ObjectId) -> Result<(), AppError> {
    let legacy = db::user_data_collection(&state.db)
        .find_one(doc! { "username_lower": username_lower })
        .await?;

    if let Some(legacy) = legacy {
        let mut bundle = match &legacy.enc_full_details {
            Some(blob) => state.encryptor.decrypt_json::<FullBackupDto>(blob)?,
            None => FullBackupDto::default(),
        };
        if bundle.profile.is_none() {
            if let Some(blob) = &legacy.enc_user_details {
                bundle.profile = Some(state.encryptor.decrypt_json::<UserProfileDto>(blob)?);
            }
        }
        store::save_bundle(&state.db, user_id, bundle).await?;
        tracing::info!(user = username_lower, "migrated legacy user_data into per-user collections");
    }

    db::users_collection(&state.db)
        .update_one(
            doc! { "_id": user_id },
            doc! { "$set": { "data_version": CURRENT_DATA_VERSION } },
        )
        .await?;
    Ok(())
}

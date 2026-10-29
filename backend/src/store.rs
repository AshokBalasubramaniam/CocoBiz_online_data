//! Per-user business data, one MongoDB collection per kind of record.
//! Every read and write here is filtered by the caller's `user_id`.

use std::collections::{HashMap, HashSet};

use mongodb::bson::{Document, doc, oid::ObjectId};
use mongodb::{Collection, Database};
use serde::Serialize;
use serde::de::DeserializeOwned;

use crate::db;
use crate::error::AppError;
use crate::models::{AppSettingsDto, DealerDto, FullBackupDto, Owned, SalesEntryDto, UserProfileDto};

async fn find_all<T>(coll: &Collection<Owned<T>>, filter: Document) -> Result<Vec<T>, AppError>
where
    T: DeserializeOwned + Send + Sync,
{
    let mut cursor = coll.find(filter).sort(doc! { "id": 1 }).await?;
    let mut rows = Vec::new();
    while cursor.advance().await? {
        rows.push(cursor.deserialize_current()?.data);
    }
    Ok(rows)
}

async fn upsert<T>(coll: &Collection<Owned<T>>, filter: Document, user_id: ObjectId, data: T) -> Result<(), AppError>
where
    T: Serialize + Send + Sync,
{
    coll.replace_one(filter, Owned { id: None, user_id, data })
        .upsert(true)
        .await?;
    Ok(())
}

// ---------- profile / settings (one row per user) ----------

pub async fn get_profile(db: &Database, user_id: ObjectId) -> Result<Option<UserProfileDto>, AppError> {
    Ok(db::profiles_collection(db)
        .find_one(doc! { "user_id": user_id })
        .await?
        .map(|row| row.data))
}

pub async fn put_profile(db: &Database, user_id: ObjectId, profile: UserProfileDto) -> Result<(), AppError> {
    upsert(&db::profiles_collection(db), doc! { "user_id": user_id }, user_id, profile).await
}

pub async fn get_settings(db: &Database, user_id: ObjectId) -> Result<Option<AppSettingsDto>, AppError> {
    Ok(db::settings_collection(db)
        .find_one(doc! { "user_id": user_id })
        .await?
        .map(|row| row.data))
}

pub async fn put_settings(db: &Database, user_id: ObjectId, settings: AppSettingsDto) -> Result<(), AppError> {
    upsert(&db::settings_collection(db), doc! { "user_id": user_id }, user_id, settings).await
}

// ---------- dealers ----------

pub async fn list_dealers(db: &Database, user_id: ObjectId) -> Result<Vec<DealerDto>, AppError> {
    find_all(&db::dealers_collection(db), doc! { "user_id": user_id }).await
}

pub async fn put_dealer(db: &Database, user_id: ObjectId, dealer: DealerDto) -> Result<(), AppError> {
    let filter = doc! { "user_id": user_id, "id": dealer.id };
    upsert(&db::dealers_collection(db), filter, user_id, dealer).await
}

/// Deletes the dealer and its sales (same cascade as the app's Room schema).
pub async fn delete_dealer(db: &Database, user_id: ObjectId, id: i64) -> Result<bool, AppError> {
    let deleted = db::dealers_collection(db)
        .delete_one(doc! { "user_id": user_id, "id": id })
        .await?
        .deleted_count;
    db::sales_collection(db)
        .delete_many(doc! { "user_id": user_id, "dealer_id": id })
        .await?;
    Ok(deleted > 0)
}

// ---------- sales ----------

pub async fn list_sales(db: &Database, user_id: ObjectId, status: Option<&str>) -> Result<Vec<SalesEntryDto>, AppError> {
    let mut filter = doc! { "user_id": user_id };
    if let Some(status) = status {
        filter.insert("status", status.to_uppercase());
    }
    find_all(&db::sales_collection(db), filter).await
}

pub async fn put_sale(db: &Database, user_id: ObjectId, sale: SalesEntryDto) -> Result<(), AppError> {
    let filter = doc! { "user_id": user_id, "id": sale.id };
    upsert(&db::sales_collection(db), filter, user_id, sale).await
}

pub async fn delete_sale(db: &Database, user_id: ObjectId, id: i64) -> Result<bool, AppError> {
    Ok(db::sales_collection(db)
        .delete_one(doc! { "user_id": user_id, "id": id })
        .await?
        .deleted_count
        > 0)
}

// ---------- whole-account bundle (what the app syncs) ----------

pub async fn load_bundle(db: &Database, user_id: ObjectId) -> Result<FullBackupDto, AppError> {
    Ok(FullBackupDto {
        dealers: list_dealers(db, user_id).await?,
        sales: list_sales(db, user_id, None).await?,
        profile: get_profile(db, user_id).await?,
        settings: get_settings(db, user_id).await?,
    })
}

/// Makes the user's rows match `bundle`: only rows that are new or changed
/// are written, and rows missing from the bundle are deleted. A missing
/// profile/settings in the bundle leaves the stored one untouched.
pub async fn save_bundle(db: &Database, user_id: ObjectId, bundle: FullBackupDto) -> Result<(), AppError> {
    sync_rows(&db::dealers_collection(db), user_id, bundle.dealers, |d| d.id).await?;
    sync_rows(&db::sales_collection(db), user_id, bundle.sales, |s| s.id).await?;
    if let Some(profile) = bundle.profile {
        if get_profile(db, user_id).await?.as_ref() != Some(&profile) {
            put_profile(db, user_id, profile).await?;
        }
    }
    if let Some(settings) = bundle.settings {
        if get_settings(db, user_id).await?.as_ref() != Some(&settings) {
            put_settings(db, user_id, settings).await?;
        }
    }
    Ok(())
}

async fn sync_rows<T>(
    coll: &Collection<Owned<T>>,
    user_id: ObjectId,
    rows: Vec<T>,
    id_of: impl Fn(&T) -> i64,
) -> Result<(), AppError>
where
    T: Serialize + DeserializeOwned + PartialEq + Send + Sync,
{
    let existing: HashMap<i64, T> = find_all(coll, doc! { "user_id": user_id })
        .await?
        .into_iter()
        .map(|row| (id_of(&row), row))
        .collect();

    let mut keep: HashSet<i64> = HashSet::new();
    for row in rows {
        let id = id_of(&row);
        keep.insert(id);
        if existing.get(&id) != Some(&row) {
            upsert(coll, doc! { "user_id": user_id, "id": id }, user_id, row).await?;
        }
    }

    let removed: Vec<i64> = existing.keys().filter(|id| !keep.contains(id)).copied().collect();
    if !removed.is_empty() {
        coll.delete_many(doc! { "user_id": user_id, "id": { "$in": removed } })
            .await?;
    }
    Ok(())
}

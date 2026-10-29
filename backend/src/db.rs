use mongodb::bson::doc;
use mongodb::options::IndexOptions;
use mongodb::{Client, Collection, Database, IndexModel};

use crate::models::{AppSettingsDto, DealerDto, Owned, SalesEntryDto, UserDataDoc, UserDoc, UserProfileDto};

pub async fn connect(uri: &str, db_name: &str) -> mongodb::error::Result<Database> {
    let mut options = mongodb::options::ClientOptions::parse(uri).await?;
    options.app_name = Some("cocobiz-backend".to_string());
    let client = Client::with_options(options)?;
    Ok(client.database(db_name))
}

pub fn users_collection(db: &Database) -> Collection<UserDoc> {
    db.collection("users")
}

/// Legacy encrypted-blob storage, only read when migrating an old account.
pub fn user_data_collection(db: &Database) -> Collection<UserDataDoc> {
    db.collection("user_data")
}

pub fn profiles_collection(db: &Database) -> Collection<Owned<UserProfileDto>> {
    db.collection("profiles")
}

pub fn settings_collection(db: &Database) -> Collection<Owned<AppSettingsDto>> {
    db.collection("settings")
}

pub fn dealers_collection(db: &Database) -> Collection<Owned<DealerDto>> {
    db.collection("dealers")
}

pub fn sales_collection(db: &Database) -> Collection<Owned<SalesEntryDto>> {
    db.collection("sales")
}

fn unique(keys: mongodb::bson::Document) -> IndexModel {
    IndexModel::builder()
        .keys(keys)
        .options(IndexOptions::builder().unique(true).build())
        .build()
}

pub async fn ensure_indexes(db: &Database) -> mongodb::error::Result<()> {
    users_collection(db)
        .create_index(unique(doc! { "username_lower": 1 }))
        .await?;
    user_data_collection(db)
        .create_index(unique(doc! { "username_lower": 1 }))
        .await?;

    // One profile / settings row per user.
    profiles_collection(db)
        .create_index(unique(doc! { "user_id": 1 }))
        .await?;
    settings_collection(db)
        .create_index(unique(doc! { "user_id": 1 }))
        .await?;

    // Dealer and sale ids are the app's own ids, unique within one user.
    dealers_collection(db)
        .create_index(unique(doc! { "user_id": 1, "id": 1 }))
        .await?;
    sales_collection(db)
        .create_index(unique(doc! { "user_id": 1, "id": 1 }))
        .await?;
    sales_collection(db)
        .create_index(IndexModel::builder().keys(doc! { "user_id": 1, "dealer_id": 1 }).build())
        .await?;
    sales_collection(db)
        .create_index(IndexModel::builder().keys(doc! { "user_id": 1, "status": 1 }).build())
        .await?;

    Ok(())
}

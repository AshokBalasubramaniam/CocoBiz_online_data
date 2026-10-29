use mongodb::bson::doc;
use mongodb::options::IndexOptions;
use mongodb::{Client, Collection, Database, IndexModel};

use crate::models::{UserDataDoc, UserDoc};

pub async fn connect(uri: &str, db_name: &str) -> mongodb::error::Result<Database> {
    let mut options = mongodb::options::ClientOptions::parse(uri).await?;
    options.app_name = Some("cocobiz-backend".to_string());
    let client = Client::with_options(options)?;
    Ok(client.database(db_name))
}

pub fn users_collection(db: &Database) -> Collection<UserDoc> {
    db.collection("users")
}

pub fn user_data_collection(db: &Database) -> Collection<UserDataDoc> {
    db.collection("user_data")
}

pub async fn ensure_indexes(db: &Database) -> mongodb::error::Result<()> {
    let unique_username = IndexModel::builder()
        .keys(doc! { "username_lower": 1 })
        .options(IndexOptions::builder().unique(true).build())
        .build();

    users_collection(db)
        .create_index(unique_username.clone())
        .await?;
    user_data_collection(db)
        .create_index(unique_username)
        .await?;

    Ok(())
}

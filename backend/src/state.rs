use std::sync::Arc;

use mongodb::Database;

use crate::cache::Cache;
use crate::config::Config;
use crate::crypto::Encryptor;

#[derive(Clone)]
pub struct AppState {
    pub db: Database,
    pub cache: Cache,
    pub config: Arc<Config>,
    pub encryptor: Arc<Encryptor>,
}

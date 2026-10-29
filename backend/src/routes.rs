use axum::Router;
use axum::routing::{get, post, put};
use tower_http::cors::CorsLayer;
use tower_http::trace::TraceLayer;

use crate::handlers::{auth, health, profile, records, reports, sync};
use crate::state::AppState;

pub fn build(state: AppState) -> Router {
    let api = Router::new()
        .route("/auth/register", post(auth::register))
        .route("/auth/login", post(auth::login))
        .route("/auth/reset-password", post(auth::reset_password))
        .route("/profile", get(profile::get_profile).put(profile::put_profile))
        .route("/settings", get(profile::get_settings).put(profile::put_settings))
        .route("/dealers", get(records::list_dealers))
        .route("/dealers/{id}", put(records::put_dealer).delete(records::delete_dealer))
        .route("/sales", get(records::list_sales))
        .route("/sales/{id}", put(records::put_sale).delete(records::delete_sale))
        .route("/sync", get(sync::get_sync).put(sync::put_sync))
        .route("/reports", get(reports::get_reports));

    Router::new()
        .route("/health", get(health::health))
        .nest("/api/v1", api)
        .layer(CorsLayer::permissive())
        .layer(TraceLayer::new_for_http())
        .with_state(state)
}

package com.cocobiz.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cocobiz.app.data.local.dao.DealerDao
import com.cocobiz.app.data.local.dao.SalesEntryDao
import com.cocobiz.app.data.local.dao.SettingsDao
import com.cocobiz.app.data.local.dao.UserDao
import com.cocobiz.app.data.local.dao.UserProfileDao
import com.cocobiz.app.data.local.entity.DealerEntity
import com.cocobiz.app.data.local.entity.SalesEntryEntity
import com.cocobiz.app.data.local.entity.SettingsEntity
import com.cocobiz.app.data.local.entity.UserEntity
import com.cocobiz.app.data.local.entity.UserProfileEntity

@Database(
    entities = [
        UserProfileEntity::class,
        DealerEntity::class,
        SalesEntryEntity::class,
        SettingsEntity::class,
        UserEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun dealerDao(): DealerDao
    abstract fun salesEntryDao(): SalesEntryDao
    abstract fun settingsDao(): SettingsDao
    abstract fun userDao(): UserDao

    companion object {
        const val DATABASE_NAME = "cocobiz_db"

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sales_entries ADD COLUMN marketRate REAL NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN editCompletedEnabled INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}

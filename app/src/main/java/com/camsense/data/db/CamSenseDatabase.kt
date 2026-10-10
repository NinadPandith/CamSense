package com.camsense.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ScanEntity::class, ModuleRunEntity::class, FindingEntity::class,
        NetworkDeviceEntity::class, BleDeviceEntity::class, EvidenceItemEntity::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class CamSenseDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao
    abstract fun evidenceDao(): EvidenceDao

    companion object {
        // Every schema change bumps the version and adds a migration (Backend Schema
        // doc). Never ship fallbackToDestructiveMigration in a release build.
        fun build(context: Context): CamSenseDatabase =
            Room.databaseBuilder(context, CamSenseDatabase::class.java, "camsense.db")
                .addMigrations(MIGRATION_1_2)
                .build()

        /** v2: scan mode, room coverage and direction of findings, device address on findings. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE scans ADD COLUMN mode TEXT")
                db.execSQL("ALTER TABLE scans ADD COLUMN coverage REAL")
                db.execSQL("ALTER TABLE scans ADD COLUMN start_yaw REAL")
                db.execSQL("ALTER TABLE findings ADD COLUMN yaw REAL")
                db.execSQL("ALTER TABLE findings ADD COLUMN pitch REAL")
                db.execSQL("ALTER TABLE findings ADD COLUMN device_address TEXT")
            }
        }
    }
}

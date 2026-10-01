package com.camsense.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [ScanEntity::class, ModuleRunEntity::class, FindingEntity::class,
        NetworkDeviceEntity::class, BleDeviceEntity::class, EvidenceItemEntity::class],
    version = 1,
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
            Room.databaseBuilder(context, CamSenseDatabase::class.java, "camsense.db").build()
    }
}

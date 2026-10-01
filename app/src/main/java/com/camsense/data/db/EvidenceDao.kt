package com.camsense.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface EvidenceDao {
    @Insert suspend fun insert(items: List<EvidenceItemEntity>)

    @Query("SELECT * FROM evidence_items WHERE scan_id = :scanId ORDER BY created_at")
    suspend fun forScan(scanId: String): List<EvidenceItemEntity>

    @Query("DELETE FROM evidence_items WHERE scan_id = :scanId")
    suspend fun deleteForScan(scanId: String)
}

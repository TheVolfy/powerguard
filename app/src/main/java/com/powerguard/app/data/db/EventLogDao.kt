package com.powerguard.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: EventLogEntity)

    /** All events newest-first, used by the log screen. */
    @Query("SELECT * FROM event_log ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<EventLogEntity>>

    /** Most recent N events for dashboard summary. */
    @Query("SELECT * FROM event_log ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<EventLogEntity>

    /** Count of events — shown on dashboard badge. */
    @Query("SELECT COUNT(*) FROM event_log")
    fun observeCount(): Flow<Int>

    @Query("DELETE FROM event_log WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOlderThan(beforeTimestamp: Long)

    @Query("DELETE FROM event_log")
    suspend fun deleteAll()
}

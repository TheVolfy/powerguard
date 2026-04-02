package com.powerguard.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.powerguard.app.domain.model.EventLogItem

@Entity(tableName = "event_log")
data class EventLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val featureTypeKey: String,
    val featureDisplayName: String,
    val action: String,
    val reason: String,
    val wasDirectControl: Boolean,
) {
    fun toDomain() = EventLogItem(
        id = id,
        timestamp = timestamp,
        featureDisplayName = featureDisplayName,
        action = action,
        reason = reason,
        wasDirectControl = wasDirectControl,
    )
}

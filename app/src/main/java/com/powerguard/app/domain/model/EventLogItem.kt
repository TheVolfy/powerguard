package com.powerguard.app.domain.model

/** Domain-layer representation of a logged automation event. */
data class EventLogItem(
    val id: Int,
    val timestamp: Long,
    val featureDisplayName: String,
    val action: String,
    val reason: String,
    val wasDirectControl: Boolean,
)

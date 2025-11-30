package com.appcontrol.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_usage")
data class AppUsage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val date: String,              // yyyy-MM-dd format
    val totalUsageMillis: Long = 0 // Total usage time in milliseconds
)


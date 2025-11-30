package com.appcontrol.data.model

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: android.graphics.drawable.Drawable?,
    val isBlocked: Boolean = false
)


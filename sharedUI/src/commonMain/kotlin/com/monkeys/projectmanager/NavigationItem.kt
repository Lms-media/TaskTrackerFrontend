package com.monkeys.projectmanager

import androidx.compose.ui.graphics.vector.ImageVector

data class NavigationItem(
    val title: String,
    val icon: ImageVector,
    val hasNotification: Boolean = false
)
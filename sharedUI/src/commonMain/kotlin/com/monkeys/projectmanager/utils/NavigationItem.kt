package com.monkeys.projectmanager.utils

import androidx.compose.ui.graphics.vector.ImageVector

data class NavigationItem(
    val id: ActionType,
    val title: String,
    val icon: ImageVector,
    val hasNotification: Boolean = false
)
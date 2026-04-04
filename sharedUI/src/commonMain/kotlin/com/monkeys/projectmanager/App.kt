package com.monkeys.projectmanager

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.monkeys.projectmanager.theme.AppTheme
import com.monkeys.projectmanager.utils.getTask
import com.monkeys.projectmanager.utils.think

@Preview
@Composable
fun App(
    onThemeChanged: @Composable (isDark: Boolean) -> Unit = {}
) = AppTheme(onThemeChanged) {

    var isExpanded by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf(getTask) }

    Row(Modifier.fillMaxSize()) {
        SideNavigationDrawer(
            isExpanded = isExpanded,
            selectedItem = selectedItem,
            onItemClick = { selectedItem = it },
            onToggle = { isExpanded = !isExpanded }
        )

        MainContent(
            selectedItem,
            { selectedItem = think }
        )
    }
}
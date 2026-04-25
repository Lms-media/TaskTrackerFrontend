package com.monkeys.projectmanager

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.monkeys.projectmanager.theme.AppTheme
import com.monkeys.projectmanager.utils.ActionType
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Preview
@Composable
fun App(
    onThemeChanged: @Composable (isDark: Boolean) -> Unit = {}
) = AppTheme(onThemeChanged) {

    var isExpanded by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf(ActionType.GET_TASK) }
    var selectedTab by remember { mutableStateOf(ActionType.ENUM_END) }
    var selectedProjId by remember { mutableStateOf<Uuid?>(null) }
    var showAllProjects by remember { mutableStateOf(false) }

    Row(Modifier.fillMaxSize()) {
        SideNavigationDrawer(
            isExpanded = isExpanded,
            selectedItem = selectedItem,
            selectedTab = selectedTab,
            onItemClick = {
                selectedTab = ActionType.PROJECTS
                selectedItem = it },
            onTabClick = { selectedTab = it },
            onToggle = { isExpanded = !isExpanded }
        )

        MainContent(
            selectedItem,
            selectedTab,
            projectId = selectedProjId,
            clearId = {
                selectedProjId = null
            },
            onClickGoTo = { tab, id, showAll ->
                selectedItem = ActionType.THINK
                selectedTab = tab
                selectedProjId = id
                showAllProjects = showAll
            },
            clearShow = {
                showAllProjects = false
            },
            showAllProjects = showAllProjects
        )
    }
}
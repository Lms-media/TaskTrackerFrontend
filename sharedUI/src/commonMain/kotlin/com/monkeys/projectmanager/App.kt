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
    var selectedNoteId by remember { mutableStateOf<Uuid?>(null) }
    var showAllProjects by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }
    var simpleMode by remember { mutableStateOf(true) }
    val requestRefresh = { refreshKey += 1 }

    if (simpleMode) {
        SimpleFlowScreen(
            onSwitchToAdvanced = { simpleMode = false }
        )
        return@AppTheme
    }

    Row(Modifier.fillMaxSize()) {
        SideNavigationDrawer(
            isExpanded = isExpanded,
            selectedItem = selectedItem,
            selectedTab = selectedTab,
            refreshKey = refreshKey,
            onItemClick = { item, noteId ->
                selectedTab = ActionType.PROJECTS
                selectedNoteId = noteId
                selectedItem = item },
            onTabClick = { selectedTab = it },
            onToggle = { isExpanded = !isExpanded },
            onSwitchToSimple = { simpleMode = true },
            onDataChanged = requestRefresh
        )

        MainContent(
            selectedItem,
            selectedTab,
            refreshKey = refreshKey,
            projectId = selectedProjId,
            noteId = selectedNoteId,
            clearId = {
                selectedProjId = null
                selectedNoteId = null
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
            showAllProjects = showAllProjects,
            onGoToTasks = {
                selectedItem = ActionType.GET_TASK
                selectedTab = ActionType.ENUM_END
                selectedProjId = null
                selectedNoteId = null
                showAllProjects = false
            },
            onDataChanged = requestRefresh
        )
    }
}

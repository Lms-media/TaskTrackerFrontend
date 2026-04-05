package com.monkeys.projectmanager

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.layout.BeyondBoundsLayout
import com.monkeys.projectmanager.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import monkeys_pm.sharedui.generated.resources.Res
import monkeys_pm.sharedui.generated.resources.note_saved
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Composable
fun MainContent(
    selectedItem: Int,
    selectedTab: Int,
    clearShow: () -> Unit,
    showAllProjects: Boolean,
    clearId: () -> Unit,
    projectId: Uuid? = null,
    onClickGoTo: (Int, Uuid?, Boolean) -> Unit,
) {
    val activeTasks by remember {
        derivedStateOf {
            LocalApi.getTasks().filter { it.status == statusActive }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val snackMessage = stringResource(Res.string.note_saved)

    LaunchedEffect(Unit) {
        while (true) {
            val currentTime = Clock.System.now().toEpochMilliseconds()

            LocalApi.getTasks()
                .filter { it.status == statusBlocked }
                .forEach { task ->
                    if (task.blockedUntil < currentTime) {
                        LocalApi.closeTask(task.id)
                    }
                }

            delay(30_000L.milliseconds)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
        AnimatedContent(
            targetState = selectedItem to selectedTab,
            transitionSpec = {
                (fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.95f))
                    .togetherWith(fadeOut(animationSpec = tween(200)))
            },
            label = "ScreenTransition"
        ) { (targetItem, targetTab) ->
            when (targetItem) {
                getTask -> {
                    if (activeTasks.isNotEmpty()) showTask(activeTasks.random())
                    else showGoTo(onClickGoTo)
                }
                editLast -> {
                    val lastNote = remember(LocalApi.getNotes()) {
                        LocalApi.getNotes().maxByOrNull { it.createdDate }
                    }
                    if (lastNote != null) {
                        EditNoteScreen(
                            note = lastNote,
                            onSave = {
                                scope.launch {
                                    snackbarHostState.showSnackbar(snackMessage)
                                }
                            }
                        )
                    }
                }
                think -> {
                    when (targetTab) {
                        tabProjects -> ProjectsScreen(
                            clearId = clearId,
                            id = projectId,
                            clearShow = clearShow,
                            showAllProjects = showAllProjects,
                            onClickGoTo = onClickGoTo,
                        )
                        tabNotes -> NotesScreen(
                            onProjectCreate = {onClickGoTo(tabProjects, it, false)},
                        )
                    }
                }
            }
        }
    }
}
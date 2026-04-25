package com.monkeys.projectmanager

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
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
    selectedItem: ActionType,
    selectedTab: ActionType,
    clearShow: () -> Unit,
    showAllProjects: Boolean,
    clearId: () -> Unit,
    projectId: Uuid? = null,
    onClickGoTo: (ActionType, Uuid?, Boolean) -> Unit,
) {
    val activeTasks by remember {
        derivedStateOf {
            ApiAdapter.getTasks().filter {
                (it.status == TaskStatus.ACTIVE || it.status == TaskStatus.ACTIVE_CURRENT)
                        && it.wave == WaveStatus.ACTIVE
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val snackMessage = stringResource(Res.string.note_saved)

    LaunchedEffect(Unit) {
        while (true) {
            val currentTime = Clock.System.now().toEpochMilliseconds()

            ApiAdapter.getTasks()
                .filter { it.status == TaskStatus.BLOCKED }
                .forEach { task ->
                    if (task.blockedUntil < currentTime) {
                        ApiAdapter.closeTask(task.id)
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
                ActionType.GET_TASK -> {
                    val currentTask = remember(activeTasks) {
                        activeTasks.find { it.status == TaskStatus.ACTIVE_CURRENT }
                            ?: activeTasks.randomOrNull()
                    }
                    if (currentTask != null && currentTask.status != TaskStatus.ACTIVE_CURRENT) {
                        currentTask.status = TaskStatus.ACTIVE_CURRENT
                        ApiAdapter.editTask(currentTask)
                    }
                    AnimatedContent(
                        targetState = currentTask,
                        transitionSpec = {
                            (slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { -it } + fadeOut())
                        },
                        label = "TaskAnimation"
                    ) { targetTask ->
                        if (targetTask != null) {
                            showTask(task = targetTask)
                        } else {
                            showGoTo(onClickGoTo)
                        }
                    }
                    // if (activeTasks.isNotEmpty()) showTask(activeTasks.random())
                    // else showGoTo(onClickGoTo)
                }

                ActionType.EDIT_LAST -> {
                    val lastNote = remember(ApiAdapter.getNotes()) {
                        ApiAdapter.getNotes().maxByOrNull { it.createdDate }
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

                ActionType.THINK -> {
                    when (targetTab) {
                        ActionType.PROJECTS -> ProjectsScreen(
                            clearId = clearId,
                            id = projectId,
                            clearShow = clearShow,
                            showAllProjects = showAllProjects,
                            onClickGoTo = onClickGoTo,
                        )

                        ActionType.NOTES -> NotesScreen(
                            onProjectCreate = { onClickGoTo(ActionType.PROJECTS, it, false) },
                        )

                        else -> {}
                    }
                }

                else -> {}
            }
        }
    }
}
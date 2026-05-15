package com.monkeys.projectmanager

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import com.monkeys.projectmanager.models.*
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
    refreshKey: Int,
    clearShow: () -> Unit,
    showAllProjects: Boolean,
    clearId: () -> Unit,
    projectId: Uuid? = null,
    noteId: Uuid? = null,
    onClickGoTo: (ActionType, Uuid?, Boolean) -> Unit,
    onGoToTasks: () -> Unit,
    onDataChanged: () -> Unit,
) {
    var tasks by remember { mutableStateOf<List<Task>>(emptyList()) }
    var notes by remember { mutableStateOf<List<Note>>(emptyList()) }
    var projects by remember { mutableStateOf<List<Project>>(emptyList()) }

    val activeTasks by remember(tasks) {
        derivedStateOf {
            tasks.filter {
                (it.status == TaskStatus.ACTIVE || it.status == TaskStatus.ACTIVE_CURRENT)
                        && it.wave == WaveStatus.ACTIVE
            }
        }
    }
    val hasNotes by remember(notes) {
        derivedStateOf { notes.isNotEmpty() }
    }
    val hasProjectsWithoutSelectedTask by remember(projects) {
        derivedStateOf {
            val now = Clock.System.now().toEpochMilliseconds()
            projects.any { it.blocksTaskReview(now) }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val snackMessage = stringResource(Res.string.note_saved)

    LaunchedEffect(refreshKey) {
        tasks = ApiAdapter.getTasks()
        notes = ApiAdapter.getNotes()
        projects = ApiAdapter.getProjects()
    }

    LaunchedEffect(Unit) {
        while (true) {
            val currentTime = Clock.System.now().toEpochMilliseconds()

            tasks
                .filter { it.status == TaskStatus.BLOCKED && it.blockedUntil <= currentTime }
                .forEach { task ->
                    ApiAdapter.closeTask(task.id)
                    onDataChanged()
                }

            tasks = ApiAdapter.getTasks()
            projects = ApiAdapter.getProjects()

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
                    if (activeTasks.isEmpty() && hasNotes) {
                        showGoToNotes { onClickGoTo(ActionType.NOTES, null, false) }
                        return@AnimatedContent
                    }

                    if (activeTasks.isEmpty() && hasProjectsWithoutSelectedTask) {
                        showGoTo(onClickGoTo)
                        return@AnimatedContent
                    }

                    val currentTask = activeTasks.find { it.status == TaskStatus.ACTIVE_CURRENT }
                        ?: activeTasks.minByOrNull { it.createdDate }
                    LaunchedEffect(currentTask?.id) {
                        if (currentTask != null && currentTask.status != TaskStatus.ACTIVE_CURRENT) {
                            currentTask.status = TaskStatus.ACTIVE_CURRENT
                            ApiAdapter.editTask(currentTask)
                            tasks = ApiAdapter.getTasks()
                            onDataChanged()
                        }
                    }
                    AnimatedContent(
                        targetState = currentTask?.id,
                        transitionSpec = {
                            (slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { -it } + fadeOut())
                        },
                        label = "TaskAnimation"
                    ) { targetTaskId ->
                        val targetTask = activeTasks.firstOrNull { it.id == targetTaskId }
                        if (targetTask != null) {
                            showTask(
                                task = targetTask,
                                onTaskChanged = {
                                    tasks = ApiAdapter.getTasks()
                                    notes = ApiAdapter.getNotes()
                                    projects = ApiAdapter.getProjects()
                                    onDataChanged()
                                }
                            )
                        } else {
                            showGoTo(onClickGoTo)
                        }
                    }
                }

                ActionType.EDIT_LAST -> {
                    LaunchedEffect(noteId) {
                        notes = ApiAdapter.getNotes()
                    }
                    val noteToEdit = remember(notes, noteId) {
                        notes.firstOrNull { it.id == noteId } ?: notes.maxByOrNull { it.createdDate }
                    }
                    if (noteToEdit != null) {
                        EditNoteScreen(
                            note = noteToEdit,
                            onSave = {
                                scope.launch {
                                    snackbarHostState.showSnackbar(snackMessage)
                                    notes = ApiAdapter.getNotes()
                                    onDataChanged()
                                }
                            }
                        )
                    } else {
                        NotesScreen(
                            onProjectCreate = {
                                onDataChanged()
                                onClickGoTo(ActionType.PROJECTS, it, false)
                            },
                            onGoToProjects = { onClickGoTo(ActionType.PROJECTS, null, false) },
                            refreshKey = refreshKey,
                            onDataChanged = onDataChanged,
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
                            refreshKey = refreshKey,
                            onClickGoTo = onClickGoTo,
                            onGoToTasks = onGoToTasks,
                            onDataChanged = onDataChanged,
                        )

                        ActionType.NOTES -> NotesScreen(
                            onProjectCreate = {
                                onDataChanged()
                                onClickGoTo(ActionType.PROJECTS, it, false)
                            },
                            onGoToProjects = { onClickGoTo(ActionType.PROJECTS, null, false) },
                            refreshKey = refreshKey,
                            onDataChanged = onDataChanged,
                        )

                        ActionType.MORNING -> MorningReviewScreen(
                            refreshKey = refreshKey,
                            onGoToTasks = onGoToTasks
                        )

                        else -> {}
                    }
                }

                ActionType.MORNING -> {
                    MorningReviewScreen(
                        refreshKey = refreshKey,
                        onGoToTasks = onGoToTasks
                    )
                }

                else -> {}
            }
        }
    }
}

package com.monkeys.projectmanager

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monkeys.projectmanager.models.Note
import com.monkeys.projectmanager.models.Project
import com.monkeys.projectmanager.models.Task
import com.monkeys.projectmanager.utils.ActionType
import com.monkeys.projectmanager.utils.ApiAdapter
import com.monkeys.projectmanager.utils.TaskStatus
import com.monkeys.projectmanager.utils.WaveStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private enum class SimpleStep {
    TASKS,
    NOTES,
    PROJECTS,
    MORNING,
    EDIT_NOTE
}

@OptIn(ExperimentalUuidApi::class)
@Composable
fun SimpleFlowScreen(
    onSwitchToAdvanced: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(SimpleStep.TASKS) }
    var tasks by remember { mutableStateOf<List<Task>>(emptyList()) }
    var notes by remember { mutableStateOf<List<Note>>(emptyList()) }
    var projects by remember { mutableStateOf<List<Project>>(emptyList()) }
    var refreshKey by remember { mutableStateOf(0) }
    var createNoteAlert by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<Note?>(null) }
    var returnStepAfterEdit by remember { mutableStateOf(SimpleStep.TASKS) }
    var showProjectListRequest by remember { mutableStateOf(0) }
    var now by remember { mutableStateOf(Clock.System.now().toEpochMilliseconds()) }
    var projectIdToOpen by remember { mutableStateOf<Uuid?>(null) }

    suspend fun reload() {
        tasks = ApiAdapter.getTasks()
        notes = ApiAdapter.getNotes()
        projects = ApiAdapter.getProjects()
    }

    fun refresh() {
        refreshKey += 1
    }

    val activeTasks by remember(tasks) {
        derivedStateOf {
            tasks.filter {
                it.wave == WaveStatus.ACTIVE &&
                        (it.status == TaskStatus.ACTIVE || it.status == TaskStatus.ACTIVE_CURRENT)
            }
        }
    }
    val hasInbox by remember(notes) { derivedStateOf { notes.isNotEmpty() } }
    val hasProjectsToPrepare by remember(projects, now) {
        derivedStateOf {
            projects.any { it.needsTaskSelection(now) }
        }
    }
    val firstProjectToPrepare = remember(projects, now) {
        projects.firstOrNull { it.needsTaskSelection(now) }?.id
    }

    LaunchedEffect(Unit) {
        while (true) {
            now = Clock.System.now().toEpochMilliseconds()
            delay(1_000L.milliseconds)
        }
    }

    LaunchedEffect(refreshKey) {
        reload()
        if (step == SimpleStep.NOTES && notes.isEmpty()) {
            if (activeTasks.isNotEmpty()) {
                step = SimpleStep.TASKS
            } else {
                val currentTime = Clock.System.now().toEpochMilliseconds()
                projectIdToOpen = projects.firstOrNull { it.needsTaskSelection(currentTime) }?.id
                step = SimpleStep.PROJECTS
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFFAFAFA)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            SimpleFixedActionsRail(
                hasNotes = notes.isNotEmpty(),
                showProjectList = step == SimpleStep.PROJECTS,
                onCreateNote = { createNoteAlert = true },
                onEditNote = {
                    returnStepAfterEdit = step
                    noteToEdit = notes.maxByOrNull { it.createdDate }
                    step = SimpleStep.EDIT_NOTE
                },
                onShowProjectList = {
                    projectIdToOpen = null
                    showProjectListRequest += 1
                },
                onMorningReview = { step = SimpleStep.MORNING },
                onSwitchToAdvanced = onSwitchToAdvanced
            )
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            when (step) {
                SimpleStep.TASKS -> {
                    val currentTask = activeTasks.find { it.status == TaskStatus.ACTIVE_CURRENT }
                        ?: activeTasks.minByOrNull { it.createdDate }

                    LaunchedEffect(currentTask?.id) {
                        if (currentTask != null && currentTask.status != TaskStatus.ACTIVE_CURRENT) {
                            currentTask.status = TaskStatus.ACTIVE_CURRENT
                            ApiAdapter.editTask(currentTask)
                            reload()
                        }
                    }

                    if (currentTask != null) {
                        showTask(
                            task = currentTask,
                            onTaskChanged = {
                                reload()
                                refresh()
                            }
                        )
                    } else {
                        SimpleTasksGateScreen(
                            hasInbox = hasInbox,
                            hasProjectsToPrepare = hasProjectsToPrepare,
                            onGoToInbox = { step = SimpleStep.NOTES },
                            onGoToProjects = {
                                projectIdToOpen = firstProjectToPrepare
                                step = SimpleStep.PROJECTS
                            }
                        )
                    }
                }

                SimpleStep.NOTES -> {
                    NotesScreen(
                        onProjectCreate = {
                            scope.launch {
                                reload()
                            }
                        },
                        onGoToProjects = {
                            projectIdToOpen = firstProjectToPrepare
                            step = SimpleStep.PROJECTS
                        },
                        refreshKey = refreshKey,
                        onDataChanged = {
                            scope.launch {
                                reload()
                                refresh()
                            }
                        },
                        showGoToProjectsOnlyWhenEmpty = true
                    )
                }

                SimpleStep.PROJECTS -> {
                    ProjectsScreen(
                        clearId = {},
                        clearShow = {},
                        showAllProjects = false,
                        refreshKey = refreshKey,
                        id = projectIdToOpen,
                        onClickGoTo = { _, _, _ -> },
                        onGoToTasks = {
                            step = if (notes.isNotEmpty()) SimpleStep.NOTES else SimpleStep.TASKS
                        },
                        showProjectListRequest = showProjectListRequest,
                        onDataChanged = {
                            scope.launch {
                                reload()
                                refresh()
                            }
                        },
                        simpleMode = true
                    )
                }

                SimpleStep.EDIT_NOTE -> {
                    val editableNote = noteToEdit
                    if (editableNote == null) {
                        step = returnStepAfterEdit
                    } else {
                        EditNoteScreen(
                            note = editableNote,
                            onSave = {
                                scope.launch {
                                    reload()
                                    step = returnStepAfterEdit
                                }
                            }
                        )
                    }
                }

                SimpleStep.MORNING -> {
                    MorningReviewScreen(
                        refreshKey = refreshKey,
                        onGoToTasks = { step = SimpleStep.TASKS }
                    )
                }
            }
        }
        }
    }

    if (createNoteAlert) {
        CreateNoteAlert(
            onDismiss = { createNoteAlert = false },
            onConfirm = { title, description ->
                scope.launch {
                    ApiAdapter.createNote(title, description)
                    createNoteAlert = false
                    reload()
                    refresh()
                }
            }
        )
    }
}

@Composable
fun ModeSlider(
    isMonkeyMode: Boolean,
    onSelectSmart: () -> Unit,
    onSelectMonkey: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onToggle = if (isMonkeyMode) onSelectSmart else onSelectMonkey
    var isHolding by remember { mutableStateOf(false) }
    val holdProgress by animateFloatAsState(
        targetValue = if (isHolding) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isHolding) 1200 else 180,
            easing = LinearEasing
        ),
        label = "ModeSliderHoldProgress"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (isMonkeyMode) 62.dp else 6.dp,
        animationSpec = tween(durationMillis = 260),
        label = "ModeSliderThumb"
    )
    val smartTint by animateColorAsState(
        targetValue = if (isMonkeyMode) Color.White else Color(0xFF3B2D60),
        animationSpec = tween(durationMillis = 260),
        label = "SmartModeTint"
    )
    val monkeyTint by animateColorAsState(
        targetValue = if (isMonkeyMode) Color(0xFF3B2D60) else Color.White,
        animationSpec = tween(durationMillis = 260),
        label = "MonkeyModeTint"
    )

    LaunchedEffect(holdProgress, isHolding) {
        if (holdProgress >= 1f && isHolding) {
            isHolding = false
            onToggle()
        }
    }

    Box(
        modifier = modifier
            .width(116.dp)
            .height(60.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(Color(0xFF3B2D60))
            .pointerInput(isMonkeyMode) {
                detectTapGestures(
                    onPress = {
                        try {
                            isHolding = true
                            awaitRelease()
                        } finally {
                            isHolding = false
                        }
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(holdProgress)
                .background(Color.White.copy(alpha = 0.18f))
        )
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White)
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "умный",
                    tint = smartTint,
                    modifier = Modifier.size(34.dp)
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "\uD83C\uDF4C",
                    color = monkeyTint,
                    fontSize = 30.sp
                )
            }
        }
    }
}

@Composable
fun ModeHoldButton(
    icon: ImageVector,
    title: String,
    isExpanded: Boolean,
    onSwitch: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isHolding by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (isHolding) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isHolding) 1200 else 180,
            easing = LinearEasing
        ),
        label = "ModeHoldProgress"
    )

    LaunchedEffect(progress, isHolding) {
        if (progress >= 1f && isHolding) {
            isHolding = false
            onSwitch()
        }
    }

    Box(
        modifier = modifier
            .background(Color(0xFFE9E9E9))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        try {
                            isHolding = true
                            awaitRelease()
                        } finally {
                            isHolding = false
                        }
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .background(Color(0xFF3B2D60).copy(alpha = 0.22f))
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.Black,
                modifier = Modifier.size(24.dp)
            )
            if (isExpanded) {
                Spacer(Modifier.width(16.dp))
                Text(
                    text = title,
                    color = Color.Black,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun SimpleFixedActionsRail(
    hasNotes: Boolean,
    showProjectList: Boolean,
    onCreateNote: () -> Unit,
    onEditNote: () -> Unit,
    onShowProjectList: () -> Unit,
    onMorningReview: () -> Unit,
    onSwitchToAdvanced: () -> Unit
) {
    Surface(
        modifier = Modifier.width(60.dp).fillMaxHeight(),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SimpleRailButton(
                icon = Icons.Default.BookmarkBorder,
                enabled = true,
                onClick = onCreateNote
            )
            SimpleRailButton(
                icon = Icons.Default.Edit,
                enabled = hasNotes,
                onClick = onEditNote
            )
            SimpleRailButton(
                icon = Icons.Default.QueryStats,
                enabled = true,
                onClick = onMorningReview
            )
            if (showProjectList) {
                Spacer(Modifier.height(14.dp))
                SimpleRailButton(
                    icon = Icons.Default.Folder,
                    enabled = true,
                    onClick = onShowProjectList
                )
            }
            Spacer(Modifier.weight(1f))
            ModeHoldButton(
                icon = Icons.Default.Lightbulb,
                title = "Умный",
                isExpanded = false,
                onSwitch = onSwitchToAdvanced,
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 12.dp)
                    .size(42.dp)
                    .clip(CircleShape)
            )
        }
    }
}

@Composable
private fun SimpleRailButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val background = if (enabled) Color(0xFFE9E9E9) else Color(0xFFF1F1F1)
    val content = if (enabled) Color.Black else Color.Gray

    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .size(42.dp)
            .clip(CircleShape)
            .background(background)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun SimpleSideActionsRail(
    hasNotes: Boolean,
    onCreateNote: () -> Unit,
    onEditNote: () -> Unit,
    onSwitchToAdvanced: () -> Unit
) {
    Surface(
        modifier = Modifier.width(60.dp).fillMaxHeight(),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onSwitchToAdvanced,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE9E9E9),
                    contentColor = Color.Black
                )
            ) {
                Text("Сложный режим")
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onCreateNote,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3B2D60),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Создать заметку")
            }
            Button(
                onClick = onEditNote,
                enabled = hasNotes,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3B2D60),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFE9E9E9),
                    disabledContentColor = Color.Gray
                ),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Редактировать заметку")
            }
        }
    }
}

@Composable
private fun SimpleTasksGateScreen(
    hasInbox: Boolean,
    hasProjectsToPrepare: Boolean,
    onGoToInbox: () -> Unit,
    onGoToProjects: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Нет задач для разбора",
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFFA9A9A9),
                textAlign = TextAlign.Center
            )
        }

        Button(
            onClick = onGoToInbox,
            enabled = hasInbox,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3B2D60),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFE9E9E9),
                disabledContentColor = Color.Gray
            )
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Разобрать инбокс")
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onGoToProjects,
            enabled = true,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3B2D60),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFE9E9E9),
                disabledContentColor = Color.Gray
            )
        ) {
            Icon(Icons.Default.Folder, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Перейти к проектам")
        }
    }
}

@Composable
private fun SimpleEmptyTasksScreen(
    hasInbox: Boolean,
    hasProjectsToPrepare: Boolean,
    onGoToInbox: () -> Unit,
    onGoToProjects: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Нет задач для разбора",
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFFA9A9A9),
                textAlign = TextAlign.Center
            )
        }

        if (hasInbox) {
            Button(
                onClick = onGoToInbox,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3B2D60),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Разобрать инбокс")
            }
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = onGoToProjects,
            enabled = hasProjectsToPrepare || !hasInbox,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3B2D60),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFF493D68),
                disabledContentColor = Color.LightGray
            )
        ) {
            Icon(Icons.Default.Folder, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Перейти к проектам")
        }
    }
}

@Composable
fun AdvancedModeSwitchButton(
    onSwitchToSimple: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onSwitchToSimple,
        modifier = modifier.padding(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFE9E9E9),
            contentColor = Color.Black
        )
    ) {
        Text("Глупый режим")
    }
}

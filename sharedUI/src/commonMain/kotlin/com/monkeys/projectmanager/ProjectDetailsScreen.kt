package com.monkeys.projectmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monkeys.projectmanager.models.Mark
import com.monkeys.projectmanager.models.Project
import com.monkeys.projectmanager.models.Task
import com.monkeys.projectmanager.utils.*
import kotlinx.coroutines.launch
import monkeys_pm.sharedui.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import kotlin.collections.emptyList
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Composable
fun ProjectDetailsScreen(
    project: Project,
    onBack: () -> Unit,
    clearShow: () -> Unit,
    showAllProjects: Boolean,
    onClickGoTo: (ActionType, Uuid?, Boolean) -> Unit,
    simpleMode: Boolean = false,
    onProjectChanged: suspend () -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    val tasks by remember(project) {
        derivedStateOf { project.tasks }
    }

    val hasActiveTask by remember(project) {
        derivedStateOf {
            tasks.any {
                it.wave == WaveStatus.ACTIVE &&
                        (it.status == TaskStatus.ACTIVE || it.status == TaskStatus.ACTIVE_CURRENT)
            }
        }
    }
    val activeBlockTask by remember(project) {
        derivedStateOf {
            tasks.find {
                it.status == TaskStatus.BLOCKED && it.blockedUntil > Clock.System.now().toEpochMilliseconds()
            }
        }
    }
    val lastTask by remember(project) {
        derivedStateOf {
            project.tasks.maxByOrNull { it.createdDate }
        }
    }
    var showBlockInfo by remember {
        mutableStateOf(project.status == ProjectStatus.OFF_FROM_BLOCK && lastTask != null)
    }
    var dismissedBlockInfoTaskId by remember(project.id) { mutableStateOf<Uuid?>(null) }
    LaunchedEffect(project.status, lastTask?.id, activeBlockTask?.id, dismissedBlockInfoTaskId) {
        showBlockInfo = project.status == ProjectStatus.OFF_FROM_BLOCK &&
                activeBlockTask == null &&
                lastTask != null &&
                lastTask!!.id != dismissedBlockInfoTaskId
    }

    var createTask by remember { mutableStateOf(false) }
    var chooseTask by remember { mutableStateOf(false) }
    var createBlockTask by remember { mutableStateOf(false) }

    var showArchive by remember { mutableStateOf(false) }
    var createMarkAlert by remember { mutableStateOf(false) }
    var marks by remember { mutableStateOf<List<Mark>>(emptyList()) }

    val refreshMarks = {
        scope.launch {
            marks = ApiAdapter.getMarks(project.id)
        }
    }
    LaunchedEffect(project.id) {
        refreshMarks()
    }

    val closedTasks by remember(project) {
        derivedStateOf { project.tasks.filter { it.status == TaskStatus.CLOSED } }
    }
    val selectableTasks by remember(project) {
        derivedStateOf {
            project.tasks
                .filter {
                    it.status != TaskStatus.CLOSED &&
                            it.status != TaskStatus.BLOCKED &&
                            it.wave == WaveStatus.BACKLOG
                }
                .sortedByDescending { it.createdDate }
        }
    }

    val scrollState = rememberScrollState()

    suspend fun chooseTargetWave(): WaveStatus {
        val allProjects = ApiAdapter.getProjects()
        val allTasks = ApiAdapter.getTasks()
        val offProjectsCount = allProjects.count { it.status == ProjectStatus.OFF }
        val isWaveAlreadyActive = allTasks.any {
            it.wave == WaveStatus.ACTIVE && (it.status == TaskStatus.ACTIVE || it.status == TaskStatus.ACTIVE_CURRENT)
        }

        val targetWave = if (!isWaveAlreadyActive && offProjectsCount <= 1) {
            WaveStatus.ACTIVE
        } else {
            WaveStatus.WAITING
        }

        if (targetWave == WaveStatus.ACTIVE) {
            allTasks
                .filter {
                    it.wave == WaveStatus.WAITING &&
                            (it.status == TaskStatus.ACTIVE || it.status == TaskStatus.ACTIVE_CURRENT)
                }
                .forEach { task ->
                    task.wave = WaveStatus.ACTIVE
                    ApiAdapter.editTask(task)
                }
        }

        return targetWave
    }

    suspend fun finishTaskSelection() {
        onProjectChanged()
        if (showAllProjects) {
            val nextProject = ApiAdapter.getProjects()
                .find { it.status == ProjectStatus.OFF }
            if (nextProject != null) {
                onBack()
                onClickGoTo(ActionType.PROJECTS, nextProject.id, true)
            } else {
                clearShow()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()){
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .verticalScroll(scrollState)
                .blur(if (activeBlockTask != null) 12.dp else 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color(0xFF3B2D60),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.back),
                    )
                }
                if (!hasActiveTask)
                    Text(
                        text = stringResource(Res.string.no_project_full),
                        color = Color.Red,
                        fontSize = 30.sp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp),
                        textAlign = TextAlign.Center,
                        lineHeight = 36.sp
                    )
                else
                    Spacer(modifier = Modifier.weight(1f))
                DeleteHoldButton(
                    onDeleteConfirmed = {
                        scope.launch {
                            ApiAdapter.closeProject(project.id)
                            onProjectChanged()
                            onBack()
                        }
                    },
                    deleteIcon = Icons.Default.Check,
                    buttonRadius = 40.dp,
                    circleRadius = 36.dp,
                    iconSize = 24.dp
                )
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 16.dp),
                shape = RoundedCornerShape(30.dp),
                color = Color.White,
                shadowElevation = 6.dp
            ) {
                Text(
                    text = project.name,
                    modifier = Modifier.padding(vertical = 40.dp),
                    style = MaterialTheme.typography.displayMedium,
                    textAlign = TextAlign.Center,
                    color = Color.Black
                )
            }

            Text(
                text = project.description,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black,
                fontSize = 30.sp,
                modifier = Modifier.padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProjectActionButton(
                    icon = Icons.Default.Add,
                    text = "Выбрать задачу",
                    modifier = Modifier.weight(1f),
                    enabled = !hasActiveTask,
                    onClick = {
                        chooseTask = true
                    }
                )
                ProjectActionButton(
                    icon = Icons.Outlined.ChatBubbleOutline,
                    text = stringResource(Res.string.create_mark),
                    modifier = Modifier.weight(1f),
                    onClick = { createMarkAlert = true }
                )
                ProjectActionButton(
                    icon = Icons.Default.Archive,
                    text = stringResource(Res.string.archive),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        showArchive = true
                    }
                )
                ProjectActionButton(
                    icon = Icons.Outlined.AccessTime,
                    text = stringResource(Res.string.block),
                    modifier = Modifier.weight(1f),
                    enabled = !hasActiveTask,
                    onClick = {
                        createBlockTask = true
                    }
                )
            }
            Spacer(Modifier.height(16.dp))
            // Секция с пометками
            if (marks.isNotEmpty()) {
                Text(
                    text = stringResource(Res.string.marks),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    color = Color.DarkGray
                )

                marks.forEach { mark ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 2.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = mark.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = Color(0xFF3B2D60)
                            )
                            if (mark.text.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = mark.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        if (activeBlockTask != null) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black.copy(alpha = 0.3f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Outlined.AccessTime,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(Res.string.blocked_until),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = formatTime(activeBlockTask!!.blockedUntil),
                        color = Color.White,
                        style = MaterialTheme.typography.displaySmall
                    )

                    Button(
                        onClick = onBack,
                        modifier = Modifier.padding(top = 32.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3B2D60),
                            disabledContainerColor = Color(0xFF493D68),
                            disabledContentColor = Color.LightGray,
                            contentColor = Color.White
                        )
                    ) {
                        Text(stringResource(Res.string.get_back))
                    }
                }
            }
        }
    }

    if (chooseTask) {
        ChooseProjectTaskAlert(
            tasks = selectableTasks,
            onDismiss = { chooseTask = false },
            onCreateTask = {
                chooseTask = false
                createTask = true
            },
            onAddToWave = { task ->
                scope.launch {
                    val targetWave = chooseTargetWave()
                    task.status = TaskStatus.ACTIVE
                    task.wave = targetWave
                    task.blockedUntil = Clock.System.now().toEpochMilliseconds()
                    val taskUpdated = ApiAdapter.editTask(task)
                    if (!taskUpdated) return@launch

                    project.status = ProjectStatus.ON
                    ApiAdapter.editProject(project)
                    chooseTask = false
                    finishTaskSelection()
                }
            }
        )
    }

    if (createTask) {
        CreateAlert(
            onClick = {
                createTask = false
            },
            onConfirm = {name, desc ->
                scope.launch {
                    val createdTaskId = ApiAdapter.createTask(
                        project.id,
                        name,
                        desc,
                        TaskStatus.ACTIVE,
                        WaveStatus.BACKLOG,
                        Clock.System.now().toEpochMilliseconds()
                    )
                    if (createdTaskId == null) return@launch
                    onProjectChanged()
                    chooseTask = true
                    createTask = false
                }
            },
            stringResource(Res.string.create_task),
        )
    }

    if (createBlockTask) {
        BlockAlert(
            onDismiss = {
                createBlockTask = false
            },
            onConfirm = {name, description, date ->
                scope.launch {
                    chooseTargetWave()

                    val blockTaskId = ApiAdapter.blockProject(
                        project.id,
                        name,
                        description,
                        date - timeZone
                    )
                    if (blockTaskId == null) return@launch
                    onProjectChanged()
                    createBlockTask = false
                }
            }
        )
    }

    if (showArchive) {
        ArchiveAlert(
            tasks = closedTasks,
            onDismiss = { showArchive = false }
        )
    }

    if (showBlockInfo && lastTask != null) {
        AlertDialog(
            onDismissRequest = { showBlockInfo = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = Color.White,
            title = {
                Text(
                    text = lastTask!!.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = lastTask!!.description,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(
                            Res.string.blocked_task_from,
                            formatTime(lastTask!!.createdDate)
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            dismissedBlockInfoTaskId = lastTask!!.id
                            project.status = ProjectStatus.OFF
                            ApiAdapter.editProject(project)
                            onProjectChanged()
                            showBlockInfo = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B2D60))
                ) {
                    Text(stringResource(Res.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        dismissedBlockInfoTaskId = lastTask!!.id
                        showBlockInfo = false
                        onBack()
                    }
                ) {
                    Text(stringResource(Res.string.back), color = Color(0xFF3B2D60))
                }
            }
        )
    }

    if (createMarkAlert)
        CreateNoteAlert(
            onDismiss = {createMarkAlert = false},
            onConfirm = {title, description ->
                scope.launch {
                    ApiAdapter.createMark(project.id, title, description)
                    refreshMarks()
                    createMarkAlert = false
                }
            }
        )
}

@OptIn(ExperimentalUuidApi::class)
@Composable
fun ChooseProjectTaskAlert(
    tasks: List<Task>,
    onDismiss: () -> Unit,
    onCreateTask: () -> Unit,
    onAddToWave: (Task) -> Unit
) {
    var selectedTask by remember(tasks) { mutableStateOf(tasks.firstOrNull()) }
    var showConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.White,
        title = { Text("Выбрать задачу") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onCreateTask,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3B2D60),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.create_task))
                }

                if (tasks.isEmpty()) {
                    Text(
                        text = "У проекта пока нет задач для выбора",
                        color = Color.Gray,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        textAlign = TextAlign.Center
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tasks, key = { it.id }) { task ->
                            val selected = selectedTask?.id == task.id
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedTask = task },
                                shape = RoundedCornerShape(16.dp),
                                color = if (selected) Color(0xFFEDE7F6) else Color(0xFFF5F5F5)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.Black
                                    )
                                    if (selected) {
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = task.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel), color = Color(0xFF3B2D60))
            }
        },
        dismissButton = {
            IconButton(
                onClick = { if (selectedTask != null) showConfirm = true },
                enabled = selectedTask != null,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color(0xFF3B2D60),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFE9E9E9),
                    disabledContentColor = Color.Gray
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
            }
        }
    )

    if (showConfirm && selectedTask != null) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = Color.White,
            title = { Text("Добавить задачу в волну?") },
            text = {
                Text(
                    text = selectedTask!!.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Black
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirm = false
                        onAddToWave(selectedTask!!)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B2D60))
                ) {
                    Text(stringResource(Res.string.ok), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text(stringResource(Res.string.cancel), color = Color(0xFF3B2D60))
                }
            }
        )
    }
}

@Composable
fun ProjectActionButton(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = { onClick() },
        enabled = enabled,
        modifier = modifier.height(50.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF3B2D60),
            disabledContainerColor = Color(0xFF493D68),
            disabledContentColor = Color.LightGray,
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            maxLines = 1
        )
    }
}

fun formatTime(millis: Long): String {
    val instant = Instant.fromEpochMilliseconds(millis + timeZone)
    return instant.toString()
}

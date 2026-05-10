package com.monkeys.projectmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monkeys.projectmanager.models.Task
import com.monkeys.projectmanager.utils.ActionType
import com.monkeys.projectmanager.utils.ApiAdapter
import com.monkeys.projectmanager.utils.ProjectStatus
import kotlinx.coroutines.launch
import monkeys_pm.sharedui.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Composable
fun showTask(
    task: Task,
    onTaskChanged: suspend () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    var showConfirmDialog by remember { mutableStateOf(false) }
    var createNoteAlert by remember { mutableStateOf(false) }
    var createProjectAlert by remember { mutableStateOf(false) }

    if (showConfirmDialog) {
        TaskConfirmationAlert(
            onDismiss = { showConfirmDialog = false },
            onConfirm = {
                scope.launch {
                    showConfirmDialog = false
                    ApiAdapter.closeTask(task.id)
                    onTaskChanged()
                }
            },
            onCreateProject = { createProjectAlert = true },
            onCreateNote = { createNoteAlert = true },
        )
    }
    if (createNoteAlert) {
        CreateNoteAlert(
            onDismiss = { createNoteAlert = false },
            onConfirm = { title, description ->
                scope.launch {
                    createNoteAlert = false
                    ApiAdapter.createNote(title, description)
                    onTaskChanged()
                }
            }
        )
    }
    if (createProjectAlert) {
        CreateAlert(
            onClick = {
                createProjectAlert = false
            },
            onConfirm = {name, desc ->
                scope.launch {
                    ApiAdapter.createProject(name, desc)
                    createProjectAlert = false
                    onTaskChanged()
                }
            },
            stringResource(Res.string.create_project),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            shadowElevation = 6.dp
        ) {
            Text(
                text = task.title,
                modifier = Modifier
                    .padding(vertical = 35.dp, horizontal = 20.dp),
                style = MaterialTheme.typography.displayMedium,
                fontSize = 42.sp,
                textAlign = TextAlign.Center,
                color = Color.Black
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(25.dp)
                    .verticalScroll(scrollState)
                    .fillMaxSize()
            ) {
                Text(
                    text = stringResource(Res.string.task_description),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 20.sp,
                    lineHeight = 28.sp,
                    color = Color.Black
                )
            }
        }

        Button(
            onClick = {
                showConfirmDialog = true
                // ApiAdapter.closeTask(task.id)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .height(65.dp),
            shape = RoundedCornerShape(22.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3B2D60),
                contentColor = Color.White
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(Res.string.close),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
fun showGoTo(
    onClickGoTo: (ActionType, Uuid?, Boolean) -> Unit,
) {
    var offProjectIds by remember {
        mutableStateOf<List<Uuid>>(emptyList())
    }

    LaunchedEffect(Unit) {
        offProjectIds = ApiAdapter.getProjects()
            .filter { it.status == ProjectStatus.OFF }
            .map { it.id }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFFAFAFA)
    ){
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp)
                .background(Color(0xFFFAFAFA)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(Res.string.no_tasks),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFFA9A9A9),
                    maxLines = 1
                )
            }
            ElevatedButton(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .height(50.dp)
                    .fillMaxWidth(),
                onClick = {
                    if (offProjectIds.isNotEmpty()) {
                        onClickGoTo(ActionType.PROJECTS, offProjectIds.first(), true)
                    } else {
                        onClickGoTo(ActionType.PROJECTS, null, false)
                    }
                },
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = Color(0xFF3B2D60),
                    contentColor = Color.White
                ),
                content = {
                    Icon(Icons.Default.Folder, contentDescription = null)
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(
                        stringResource(Res.string.go_to_create_tasks),
                        maxLines = 1)
                }
            )
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
fun showGoToNotes(
    onClickGoToNotes: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFFAFAFA)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp)
                .background(Color(0xFFFAFAFA)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Сначала разберите заметки",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFFA9A9A9),
                    maxLines = 1
                )
            }
            ElevatedButton(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .height(50.dp)
                    .fillMaxWidth(),
                onClick = onClickGoToNotes,
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = Color(0xFF3B2D60),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Folder, contentDescription = null)
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Перейти к заметкам", maxLines = 1)
            }
        }
    }
}

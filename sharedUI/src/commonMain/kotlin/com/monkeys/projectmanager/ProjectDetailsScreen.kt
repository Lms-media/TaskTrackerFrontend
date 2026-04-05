package com.monkeys.projectmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
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
import com.monkeys.projectmanager.models.Project
import com.monkeys.projectmanager.utils.LocalApi
import com.monkeys.projectmanager.utils.projectStatusOff
import com.monkeys.projectmanager.utils.statusActive
import com.monkeys.projectmanager.utils.statusBlocked
import com.monkeys.projectmanager.utils.statusClosed
import com.monkeys.projectmanager.utils.tabProjects
import com.monkeys.projectmanager.utils.timeZone
import monkeys_pm.sharedui.generated.resources.*
import org.jetbrains.compose.resources.stringResource
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
    onClickGoTo: (Int, Uuid?, Boolean) -> Unit
) {
    val tasks by remember(project.id) {
        derivedStateOf { project.tasks }
    }
    val hasActiveTask by remember(project.id) {
        derivedStateOf { tasks.any { it.status == statusActive } }
    }
    val activeBlockTask by remember(project.id) {
        derivedStateOf {
            tasks.find {
                it.status == statusBlocked && it.blockedUntil > Clock.System.now().toEpochMilliseconds()
            }
        }
    }

    var createTask by remember { mutableStateOf(false) }
    var createBlockTask by remember { mutableStateOf(false) }

    var showArchive by remember { mutableStateOf(false) }

    val closedTasks by remember {
        derivedStateOf { project.tasks.filter { it.status == statusClosed } }
    }

    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()){
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .verticalScroll(scrollState)
                .blur(if (activeBlockTask != null) 12.dp else 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(horizontal = 16.dp)) {
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
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = TextAlign.Center,
                        lineHeight = 36.sp
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
                    text = stringResource(Res.string.create_task),
                    modifier = Modifier.weight(1f),
                    enabled = !hasActiveTask,
                    onClick = {
                        createTask = true
                    }
                )
                ProjectActionButton(
                    icon = Icons.Outlined.ChatBubbleOutline,
                    text = stringResource(Res.string.create_mark),
                    modifier = Modifier.weight(1f),
                    onClick = { }
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

    if (createTask) {
        CreateAlert(
            onClick = {
                createTask = false
            },
            onConfirm = {name, desc ->
                LocalApi.createTask(
                    project.id,
                    name,
                    desc,
                    statusActive,
                    Clock.System.now().toEpochMilliseconds()
                )
                if (showAllProjects){
                    val nextProject = LocalApi.getProjects()
                        .find { it.status == projectStatusOff }
                    if (nextProject != null) {
                        onBack()
                        onClickGoTo(tabProjects, nextProject.id, true)
                    } else {
                        clearShow()
                    }
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
            onConfirm = {date ->
                LocalApi.blockProject(
                    project.id,
                    date - timeZone
                )
            }
        )
    }

    if (showArchive) {
        ArchiveAlert(
            tasks = closedTasks,
            onDismiss = { showArchive = false }
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
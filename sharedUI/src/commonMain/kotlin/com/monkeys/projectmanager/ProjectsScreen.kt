package com.monkeys.projectmanager

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monkeys.projectmanager.models.Project
import com.monkeys.projectmanager.utils.ActionType
import com.monkeys.projectmanager.utils.ApiAdapter
import com.monkeys.projectmanager.utils.ProjectStatus
import kotlinx.coroutines.launch
import monkeys_pm.sharedui.generated.resources.Res
import monkeys_pm.sharedui.generated.resources.create_project
import monkeys_pm.sharedui.generated.resources.no_all_projects_full
import monkeys_pm.sharedui.generated.resources.no_projects
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Composable
fun ProjectsScreen(
    clearId: () -> Unit,
    clearShow: () -> Unit,
    showAllProjects: Boolean,
    refreshKey: Int,
    id: Uuid? = null,
    onClickGoTo: (ActionType, Uuid?, Boolean) -> Unit,
    onDataChanged: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var projects by remember { mutableStateOf<List<Project>>(emptyList()) }
    var selectedProject by remember { mutableStateOf<Project?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshKey) {
        projects = ApiAdapter.getProjects()
        selectedProject?.let { project ->
            selectedProject = ApiAdapter.getProject(project.id)
        }
    }

    val hasOffProjects by remember(projects) {
        derivedStateOf {
            projects.any { it.status == ProjectStatus.OFF
                    || it.status == ProjectStatus.OFF_FROM_BLOCK
            }
        }
    }
    val sortedProjects by remember(projects) {
        derivedStateOf {
            projects.sortedWith(
                compareBy<Project> {
                    it.status != ProjectStatus.OFF && it.status != ProjectStatus.OFF_FROM_BLOCK
                }.thenByDescending { it.createdDate }
            )
        }
    }
    LaunchedEffect(id) {
        if (id != null) {
            selectedProject = ApiAdapter.getProject(id)
        }
    }

    AnimatedContent(
        targetState = selectedProject?.id,
        transitionSpec = {
            if (targetState != null) {
                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> -width } + fadeOut())
            } else {
                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> width } + fadeOut())
            }
        },
        label = "ProjectTransition"
    ) { currentProjectId ->
        val currentProject = selectedProject?.takeIf { it.id == currentProjectId }
        if (currentProject == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5))
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (sortedProjects.isNotEmpty()) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (hasOffProjects) {
                                Text(
                                    text = stringResource(Res.string.no_all_projects_full),
                                    color = Color.Red,
                                    fontSize = 30.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 36.sp
                                )
                            }

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentPadding = PaddingValues(bottom = 100.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(
                                    sortedProjects,
                                    key = { "${it.id}_${it.status}" }
                                ) { project ->
                                    println("project title: ${project.name}")
                                    println("project status: ${project.status}")

                                    ProjectItemRow(
                                        project,
                                        hasOffProjects = hasOffProjects,
                                        onClick = { selectedProject = project }
                                    )
                                }
                            }
                        }
                    } else
                        Box(
                            modifier = Modifier
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(Res.string.no_projects),
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color(0xFFA9A9A9),
                                maxLines = 1
                            )
                        }

                    ExtendedFloatingActionButton(
                        onClick = { showDialog = true },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(24.dp),
                        containerColor = Color(0xFF3B2D60),
                        contentColor = Color.White,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Outlined.Folder, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(Res.string.create_project))
                    }
                }
            }
        } else ProjectDetailsScreen(
            currentProject,
            onBack = {
                selectedProject = null
                clearId()
                clearShow()
                scope.launch {
                    projects = ApiAdapter.getProjects()
                    onDataChanged()
                }
            },
            clearShow = clearShow,
            showAllProjects = showAllProjects,
            onClickGoTo = onClickGoTo,
            onProjectChanged = {
                projects = ApiAdapter.getProjects()
                selectedProject = ApiAdapter.getProject(currentProject.id)
                onDataChanged()
            }
        )
    }

    if (showDialog) {
        CreateAlert(
            onClick = {
                showDialog = false
            },
            onConfirm = { name, desc ->
                scope.launch {
                    ApiAdapter.createProject(name, desc)
                    projects = ApiAdapter.getProjects()
                    onDataChanged()
                    showDialog = false
                }
            },
            stringResource(Res.string.create_project),
        )
    }
}

@Composable
fun ProjectItemRow(
    project: Project,
    hasOffProjects: Boolean,
    onClick: () -> Unit
) {
    println("project title in item: ${project.name}")
    println("project status in item: ${project.status}")
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .background(
                    if ((project.status == ProjectStatus.OFF || project.status == ProjectStatus.OFF_FROM_BLOCK)
                        && hasOffProjects
                    ) Color(
                        0xFFFFEBEE
                    )
                    else Color(0xFFE8F5E9)
                )
                .padding(vertical = 24.dp, horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.size(32.dp))

            Text(
                text = project.name,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.Black
            )

            if ((project.status == ProjectStatus.OFF || project.status == ProjectStatus.OFF_FROM_BLOCK) && hasOffProjects) {
                if (project.status == ProjectStatus.OFF_FROM_BLOCK && hasOffProjects) {
                    Icon(
                        imageVector = Icons.Default.HourglassEmpty,
                        contentDescription = null,
                        tint = Color(0xFF3B2D60),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                }
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFF2FC72F),
                    modifier = Modifier.size(32.dp)
                )
            } else {
                Spacer(modifier = Modifier.size(32.dp))
            }
        }
        HorizontalDivider(
            thickness = 1.dp,
            color = Color(0xFFEEEEEE),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

package com.monkeys.projectmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import com.monkeys.projectmanager.utils.LocalApi
import com.monkeys.projectmanager.utils.projectStatusOff
import monkeys_pm.sharedui.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Composable
fun ProjectsScreen(
    clearId: () -> Unit,
    clearShow: () -> Unit,
    showAllProjects: Boolean,
    id: Uuid? = null,
    onClickGoTo: (Int, Uuid?, Boolean) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val hasOffProjects by remember {
        derivedStateOf { LocalApi.getProjects().any { it.status == projectStatusOff } }
    }
    val projects by remember {
        derivedStateOf {
            hasOffProjects
            LocalApi.getProjects().sortedWith(
                compareBy<Project> { it.status != projectStatusOff }
                    .thenByDescending { it.createdDate }
            )
        }
    }
    var selectedProject by remember { mutableStateOf<Project?>(null) }

    if (id != null) {
        selectedProject = LocalApi.getProject(id)
    }

    if (selectedProject == null){
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (projects.isNotEmpty()) {
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
                                projects,
                                key = { it.id }
                            ) { project ->

                                ProjectItemRow(
                                    project,
                                    hasOffProjects = hasOffProjects,
                                    onClick = { selectedProject = project }
                                )
                            }
                        }
                    }
                }
                else
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
    }
    else ProjectDetailsScreen(
        selectedProject!!,
        onBack = {
            selectedProject = null
            clearId()
            clearShow()
        },
        clearShow = clearShow,
        showAllProjects = showAllProjects,
        onClickGoTo = onClickGoTo
    )

    if (showDialog) {
        CreateAlert(
            onClick = {
                showDialog = false
            },
            onConfirm = {name, desc ->
                LocalApi.createProject(name, desc)
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
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .background(
                    if (project.status == projectStatusOff && hasOffProjects) Color(0xFFFFEBEE)
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

            if (project.status == projectStatusOff && hasOffProjects) {
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
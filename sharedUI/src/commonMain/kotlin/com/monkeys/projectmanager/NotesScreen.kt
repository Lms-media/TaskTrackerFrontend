package com.monkeys.projectmanager

import androidx.compose.animation.*
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monkeys.projectmanager.utils.ApiAdapter
import monkeys_pm.sharedui.generated.resources.Res
import monkeys_pm.sharedui.generated.resources.create_project
import monkeys_pm.sharedui.generated.resources.delete
import monkeys_pm.sharedui.generated.resources.no_notes
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Composable
fun DeleteHoldButton(
    onDeleteConfirmed: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isHolding by remember { mutableStateOf(false) }

    val progressAnimation by animateFloatAsState(
        targetValue = if (isHolding) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isHolding) 2000 else 300,
            easing = LinearEasing
        ),
        label = "DeleteProgress"
    )

    LaunchedEffect(progressAnimation) {
        if (progressAnimation == 1f && isHolding) {
            isHolding = false
            onDeleteConfirmed()
        }
    }

    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(Color(0xFF3B2D60))
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
            },
        contentAlignment = Alignment.Center
    ) {
        if (progressAnimation > 0f) {
            CircularProgressIndicator(
                progress = { progressAnimation },
                modifier = Modifier.size(52.dp),
                color = Color(0xFFFFFFFF),
                strokeWidth = 4.dp,
                trackColor = Color.Transparent,
            )
        }

        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = stringResource(Res.string.delete),
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
fun NotesScreen(
    onProjectCreate: (Uuid) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    val notes = ApiAdapter.getNotes()

    val expandedNoteIds = remember { mutableStateListOf<Uuid>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .padding(vertical = 10.dp)
    ) {
        if (notes.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(Res.string.no_notes), color = Color.Gray, fontSize = 18.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    val isExpanded = expandedNoteIds.contains(note.id)

                    val rotationState by animateFloatAsState(
                        targetValue = if (isExpanded) 180f else 0f
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                            .padding(10.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        shadowElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isExpanded) expandedNoteIds.remove(note.id)
                                        else expandedNoteIds.add(note.id)
                                    }
                                    .padding(vertical = 8.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = note.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color.Black,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .rotate(rotationState)
                                )
                            }

                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                                ) {
                                    HorizontalDivider(color = Color.Black, thickness = 1.dp)

                                    Text(
                                        text = note.text,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontSize = 18.sp,
                                        lineHeight = 24.sp,
                                        color = Color.Black,
                                        modifier = Modifier.padding(top = 16.dp, bottom = 16.dp, end = 100.dp)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            modifier = Modifier.height(56.dp).clip(CircleShape),
                                            onClick = { showDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B2D60)),
                                            shape = RoundedCornerShape(16.dp),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                        ) {
                                            Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(stringResource(Res.string.create_project), fontSize = 14.sp)
                                        }

                                        Spacer(Modifier.width(16.dp))

                                        DeleteHoldButton(
                                            onDeleteConfirmed = {
                                                ApiAdapter.closeNote(note.id)
                                                expandedNoteIds.remove(note.id)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        CreateAlert(
            onClick = {
                showDialog = false
            },
            onConfirm = {name, desc ->
                val projId = ApiAdapter.createProject(name, desc)
                onProjectCreate(projId)
            },
            stringResource(Res.string.create_project),
        )
    }
}
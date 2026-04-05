package com.monkeys.projectmanager

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.monkeys.projectmanager.utils.timeZone
import monkeys_pm.sharedui.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi

@Composable
@OptIn(ExperimentalUuidApi::class)
fun CreateAlert(
    onClick: () -> Unit,
    onConfirm: (name: String, desc: String) -> Unit,
    title: String,
) {
    var projectName by remember { mutableStateOf("") }
    var projectDescription by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { onClick() },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text(stringResource(Res.string.name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = projectDescription,
                    onValueChange = { projectDescription = it },
                    label = { Text(stringResource(Res.string.description)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (projectName.isNotBlank()) {
                        onConfirm(projectName, projectDescription)
                        projectName = ""
                        projectDescription = ""
                        onClick()
                    }
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.White,
                    containerColor = Color(0xFF3B2D60)
                )
            ) {
                Text(stringResource(Res.string.create))
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onClick() },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.Black,
                    containerColor = Color(0xFFE9E9E9),
                    disabledContentColor = Color(0xFFE9E9E9)
                )
            ) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockAlert(
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val now = remember { Clock.System.now().toEpochMilliseconds() }
    val todayStartUtc = remember {
        now - (now % 86400000L)
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = todayStartUtc,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis >= todayStartUtc
            }
        }
    )

    val timePickerState = rememberTimePickerState(
        initialHour = 12,
        initialMinute = 0,
        is24Hour = true
    )

    var isTimePickerVisible by remember { mutableStateOf(false) }

    val finalTimestamp by remember(datePickerState.selectedDateMillis, timePickerState.hour, timePickerState.minute) {
        derivedStateOf {
            val datePart = datePickerState.selectedDateMillis ?: todayStartUtc
            val dayMs = (timePickerState.hour * 3600000L) + (timePickerState.minute * 60000L)

            datePart + dayMs
        }
    }
    val isTimeValid = finalTimestamp - timeZone > Clock.System.now().toEpochMilliseconds()

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(if (!isTimePickerVisible) stringResource(Res.string.select_date) else stringResource(Res.string.select_time)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isTimePickerVisible) {
                    DatePicker(
                        state = datePickerState,
                        showModeToggle = false,
                        title = null,
                        headline = null
                    )
                } else {
                    Spacer(Modifier.height(24.dp))
                    TimeInput(state = timePickerState)

                    if (!isTimeValid) {
                        Text(
                            text = stringResource(Res.string.time_passed),
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (!isTimePickerVisible) {
                        isTimePickerVisible = true
                    } else {
                        onConfirm(finalTimestamp)
                        onDismiss()
                    }
                },
                enabled = if (!isTimePickerVisible) datePickerState.selectedDateMillis != null else isTimeValid,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.White,
                    containerColor = Color(0xFF3B2D60)
                )
            ) {
                Text(if (!isTimePickerVisible) stringResource(Res.string.continue_button) else stringResource(Res.string.ok))
            }
        },
        dismissButton = {
            TextButton(
                onClick = { if (isTimePickerVisible) isTimePickerVisible = false else onDismiss() },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.Black,
                    containerColor = Color(0xFFE9E9E9),
                    disabledContentColor = Color(0xFFE9E9E9)
                )
            ) {
                Text(stringResource(Res.string.back))
            }
        }
    )
}

@OptIn(ExperimentalUuidApi::class)
@Composable
fun ArchiveAlert(
    tasks: List<com.monkeys.projectmanager.models.Task>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.White,
                    containerColor = Color(0xFF3B2D60),
                    disabledContentColor = Color.Gray
                )
            ) {
                Text(stringResource(Res.string.back))
            }
        },
        title = { Text(stringResource(Res.string.task_archive)) },
        text = {
            if (tasks.isEmpty()) {
                Text(stringResource(Res.string.no_task_archive), color = Color.Gray)
            } else {
                Box(modifier = Modifier.heightIn(max = 400.dp)) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(
                            tasks,
                            key = { it.id }
                        ) { task ->
                            Surface(
                                Modifier.fillMaxWidth().padding(10.dp),
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White,
                                shadowElevation = 6.dp
                            ){
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F0F0))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(text = task.title, style = MaterialTheme.typography.titleMedium)
                                        Text(text = task.description, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNoteAlert(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        content = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(32.dp),
                color = Color.White,
                shadowElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            textDecoration = TextDecoration.Underline
                        ),
                        decorationBox = { innerTextField ->
                            if (title.isEmpty()) {
                                Text(
                                    text = stringResource(Res.string.note_title),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Gray,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                            innerTextField()
                        }
                    )

                    Spacer(Modifier.height(16.dp))

                    BasicTextField(
                        value = content,
                        onValueChange = { content = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 150.dp, max = 300.dp),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 18.sp,
                            textAlign = TextAlign.Start
                        ),
                        decorationBox = { innerTextField ->
                            if (content.isEmpty()) {
                                Text(
                                    text = stringResource(Res.string.note_text),
                                    fontSize = 18.sp,
                                    color = Color.Gray
                                )
                            }
                            innerTextField()
                        }
                    )
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        IconButton(
                            onClick = {
                                if (title.isNotBlank() || content.isNotBlank()) {
                                    onConfirm(title, content)
                                    onDismiss()
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFF3B2D60), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(Res.string.save),
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    )
}
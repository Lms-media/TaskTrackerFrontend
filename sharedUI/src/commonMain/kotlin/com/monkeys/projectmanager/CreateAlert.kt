package com.monkeys.projectmanager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import monkeys_pm.sharedui.generated.resources.Res
import monkeys_pm.sharedui.generated.resources.cancel
import monkeys_pm.sharedui.generated.resources.create
import monkeys_pm.sharedui.generated.resources.description
import monkeys_pm.sharedui.generated.resources.name
import monkeys_pm.sharedui.generated.resources.new_project
import monkeys_pm.sharedui.generated.resources.select_block_date
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
                    containerColor = Color(0xFF3B2D60),
                    disabledContentColor = Color.Gray
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
                    disabledContentColor = Color.Gray
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
    val datePickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis >= Clock.System.now().toEpochMilliseconds()
            }
        }
    )

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(stringResource(Res.string.select_block_date)) },
        text = {
            DatePicker(
                state = datePickerState,
                showModeToggle = false,
                title = null,
                headline = null
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let {
                        onConfirm(it)
                        onDismiss()
                    }
                },
                enabled = datePickerState.selectedDateMillis != null,
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
                onClick = { onDismiss() },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.Black,
                    containerColor = Color(0xFFE9E9E9)
                )
            ) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}
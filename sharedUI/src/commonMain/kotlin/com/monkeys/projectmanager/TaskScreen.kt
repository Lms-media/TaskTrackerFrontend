package com.monkeys.projectmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monkeys.projectmanager.models.Task
import com.monkeys.projectmanager.utils.LocalApi
import com.monkeys.projectmanager.utils.projectStatusOff
import com.monkeys.projectmanager.utils.tabProjects
import monkeys_pm.sharedui.generated.resources.Res
import monkeys_pm.sharedui.generated.resources.close
import monkeys_pm.sharedui.generated.resources.go_to_create_tasks
import monkeys_pm.sharedui.generated.resources.no_tasks
import monkeys_pm.sharedui.generated.resources.task_description
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Composable
fun showTask(
    task: Task
) {
    val scrollState = rememberScrollState()

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
                LocalApi.closeTask(task.id)
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
    onClickGoTo: (Int, Uuid?, Boolean) -> Unit,
) {
    val offProjectIds = remember {
        LocalApi.getProjects()
            .filter { it.status == projectStatusOff }
            .map { it.id }
    }

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
                    onClickGoTo(tabProjects, offProjectIds.first(), true)
                } else {
                    onClickGoTo(tabProjects, null, false)
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

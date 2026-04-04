package com.monkeys.projectmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.monkeys.projectmanager.utils.LocalApi
import com.monkeys.projectmanager.utils.statusActive
import com.monkeys.projectmanager.utils.tabProjects
import monkeys_pm.sharedui.generated.resources.IndieFlower_Regular
import monkeys_pm.sharedui.generated.resources.Res
import monkeys_pm.sharedui.generated.resources.go_to_create_tasks
import monkeys_pm.sharedui.generated.resources.no_tasks
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.stringResource

@Composable
fun MainContent(
    selectedItem: Int,
    onClickGoTo: (Int) -> Unit,
) {
    when (selectedItem) {
        0 -> {
            val activeTasks by remember {
                derivedStateOf {
                    LocalApi.getTasks().filter { it.status == statusActive }
                }
            }
            if (activeTasks.isNotEmpty()) showTask()
            else showGoTo(onClickGoTo)
        }
    }
}

@Composable
private fun showGoTo(
    onClickGoTo: (Int) -> Unit,
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
            onClick = { onClickGoTo(tabProjects) },
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

@Composable
private fun showTask() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp)
            .background(Color(0xFFFAFAFA)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "задача есть, круто :|",
            fontFamily = FontFamily(Font(Res.font.IndieFlower_Regular)),
            style = MaterialTheme.typography.displayLarge
        )
    }
}

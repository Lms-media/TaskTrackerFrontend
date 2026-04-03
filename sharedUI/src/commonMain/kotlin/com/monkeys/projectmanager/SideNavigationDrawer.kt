package com.monkeys.projectmanager

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp


@Composable
fun SideNavigationDrawer(
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val width by animateDpAsState(if (isExpanded) 240.dp else 80.dp)

    Surface(
        modifier = Modifier.width(width).fillMaxHeight(),
        color = Color(0xFFF7F7F7),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(bottomEnd = 40.dp))
                    .background(Color(0xFF3F3361))
                    .padding(16.dp)
            ) {
                Column {
                    IconButton(onClick = onToggle) {
                        Icon(Icons.Default.Menu, contentDescription = null, tint = Color.White)
                    }
                    Spacer(Modifier.height(16.dp))
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = Color.White
                    )
                    if (isExpanded) {
                        Text(
                            "Username",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            val items = listOf(
                NavigationItem("Дай задачу", Icons.AutoMirrored.Filled.List),
                NavigationItem("Создать заметку", Icons.Default.BookmarkBorder),
                NavigationItem("Изменить", Icons.Default.Edit),
                NavigationItem("Думать", Icons.Default.Lightbulb, hasNotification = true)
            )

            items.forEach { item ->
                NavigationRow(item, isExpanded)
            }

            Spacer(Modifier.weight(1f))

            NavigationRow(
                NavigationItem("Выйти", Icons.AutoMirrored.Filled.ExitToApp),
                isExpanded,
                isBottom = true
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun NavigationRow(
    item: NavigationItem,
    isExpanded: Boolean,
    isBottom: Boolean = false
) {
    val backgroundColor = if (item.title == "Дай задачу") Color(0xFF3F3361) else Color.Transparent
    val contentColor = if (item.title == "Дай задачу") Color.White else Color.Black

    Row(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .fillMaxWidth()
            .height(50.dp)
            .clip(CircleShape)
            .background(if (isBottom) Color(0xFFE0E0E0).copy(alpha = 0.5f) else backgroundColor)
            .clickable { /* Handle click */ }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center
    ) {
        Box {
            Icon(
                item.icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            if (item.hasNotification) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .size(8.dp)
                        .background(Color.Red, CircleShape)
                )
            }
        }

        if (isExpanded) {
            Spacer(Modifier.width(16.dp))
            Text(
                text = item.title,
                color = contentColor,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1
            )
        }
    }
}
package com.monkeys.projectmanager

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.monkeys.projectmanager.models.Note
import com.monkeys.projectmanager.models.Project
import com.monkeys.projectmanager.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import monkeys_pm.sharedui.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


@OptIn(ExperimentalUuidApi::class)
@Composable
fun SideNavigationDrawer(
    isExpanded: Boolean,
    selectedItem: ActionType,
    selectedTab: ActionType,
    refreshKey: Int,
    onItemClick: (ActionType, Uuid?) -> Unit,
    onTabClick: (ActionType) -> Unit,
    onToggle: () -> Unit,
    onDataChanged: () -> Unit
) {
    var currentTime by remember { mutableStateOf(Clock.System.now().toEpochMilliseconds()) }
    var notes by remember { mutableStateOf<List<Note>>(emptyList()) }
    var projects by remember { mutableStateOf<List<Project>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshKey) {
        while (true) {
            currentTime = Clock.System.now().toEpochMilliseconds()
            notes = ApiAdapter.getNotes()
            projects = ApiAdapter.getProjects()
            delay(30000L.milliseconds)
        }
    }

    val width by animateDpAsState(if (isExpanded) 240.dp else 60.dp)
    var createNoteAlert by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.width(width).fillMaxHeight(),
        color = Color(0xFFFFFFFF),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(bottomEnd = 40.dp))
                    .background(Color(0xFF3B2D60))
                    .padding(vertical = 10.dp)
            ) {
                Column {
                    IconButton(
                        onClick = onToggle,
                        modifier = Modifier
                            .padding(start = 8.dp, end = 8.dp)
                            .size(40.dp)
                    ) {
                        Icon(Icons.Default.Menu, contentDescription = null, tint = Color.White)
                    }
                    Spacer(Modifier.height(10.dp))
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 8.dp, end = 8.dp)
                            .size(40.dp),
                        tint = Color.White
                    )
                    if (isExpanded) {
                        Text(
                            "Username",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.height(24.dp).padding(top = 5.dp, start = 10.dp, end = 10.dp)
                        )
                    }
                    else{
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            val notesNotEmpty by remember {
                derivedStateOf { notes.isNotEmpty() }
            }
            val hasOffProjects by remember {
                derivedStateOf { projects.any {
                    it.status == ProjectStatus.OFF || it.status == ProjectStatus.OFF_FROM_BLOCK
                } }
            }
            val canEditNote by remember {
                derivedStateOf {
                    val now = currentTime
                    if (!notesNotEmpty) return@derivedStateOf false
                    val lastNote = notes.maxByOrNull { it.createdDate }
                    lastNote?.let {
                        //(now - it.createdDate) <= 10_000L
                        (now - it.createdDate) <= 1_800_000L
                    } ?: false
                }
            }
            val items = buildList {
                add(NavigationItem(ActionType.GET_TASK, stringResource(Res.string.get_task), Icons.AutoMirrored.Filled.List))
                add(NavigationItem(ActionType.CREATE_NOTE, stringResource(Res.string.create_note), Icons.Default.BookmarkBorder))
                if (canEditNote)
                    add(NavigationItem(ActionType.EDIT_LAST, stringResource(Res.string.edit), Icons.Default.Edit))
                add(NavigationItem(ActionType.THINK, stringResource(Res.string.think), Icons.Default.Lightbulb))
            }

            items.forEach { item ->
                val animatedBgColor by animateColorAsState(
                    targetValue = if (selectedItem == item.id) Color(0xFF3B2D60) else Color(0xFFE9E9E9),
                    animationSpec = tween(durationMillis = 200)
                )

                if (item.id == ActionType.EDIT_LAST)
                    NavigationRow(
                        item,
                        isExpanded,
                        isSelected = item.id == selectedItem,
                        modifier = Modifier
                            .padding(top = 4.dp, bottom = 4.dp, start = 8.dp, end = 8.dp)
                            .fillMaxWidth()
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(animatedBgColor)
                            .clickable {
                                val lastNote = notes.maxByOrNull { it.createdDate }
                                onItemClick(item.id, lastNote?.id)
                            }
                            .padding(horizontal = 10.dp)
                    )
                else
                    NavigationRow(
                        item,
                        isExpanded,
                        isSelected = item.id == selectedItem,
                        modifier = Modifier
                            .padding(top = 4.dp, bottom = 4.dp, end = 8.dp)
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(bottomEnd = 100.dp, topEnd = 100.dp))
                            .background(animatedBgColor)
                            .clickable {
                                if (item.id == ActionType.CREATE_NOTE) createNoteAlert = true
                                else onItemClick(item.id, null)
                            }
                            .padding(horizontal = 8.dp)
                    )
            }

            if (selectedItem == ActionType.THINK){
                val tabs = buildList {
                    add(NavigationItem(
                        ActionType.PROJECTS,
                        stringResource(Res.string.control),
                        Icons.Outlined.Keyboard,
                        hasNotification = hasOffProjects
                    ))
                    add(NavigationItem(ActionType.NOTES,
                        stringResource(Res.string.notes),
                        Icons.Outlined.Inbox,
                        hasNotification = notesNotEmpty))
                }
                tabs.forEach { tab ->
                    val animatedBgColor by animateColorAsState(
                        targetValue = if (selectedTab == tab.id) Color(0xFF3B2D60) else Color(0xFFE9E9E9),
                        animationSpec = tween(durationMillis = 200)
                    )

                    NavigationRow(
                        tab,
                        isExpanded,
                        isSelected = tab.id == selectedTab,
                        modifier = Modifier
                            .padding(top = 4.dp, bottom = 4.dp, start = 8.dp, end = 8.dp)
                            .fillMaxWidth()
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(animatedBgColor)
                            .clickable { onTabClick(tab.id) }
                            .padding(horizontal = 10.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (createNoteAlert)
        CreateNoteAlert(
            onDismiss = {createNoteAlert = false},
            onConfirm = {title, description ->
                scope.launch {
                    createNoteAlert = false
                    ApiAdapter.createNote(title, description)
                    notes = ApiAdapter.getNotes()
                    onDataChanged()
                }
            }
        )
}

@Composable
fun NavigationRow(
    item: NavigationItem,
    isExpanded: Boolean,
    isSelected: Boolean = false,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    modifier: Modifier = Modifier
        .padding(top = 4.dp, bottom = 4.dp, end = 4.dp, start = 4.dp)
        .fillMaxWidth()
        .height(50.dp)
        .clip(CircleShape)
        .background(Color(0xFFE9E9E9))
        .clickable {  }
        .padding(horizontal = 8.dp)
) {
    val animatedContentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color.Black,
        animationSpec = tween(durationMillis = 200)
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = horizontalArrangement
    ) {
        Box {
            Icon(
                item.icon,
                contentDescription = item.title,
                tint = animatedContentColor,
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
                color = animatedContentColor,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1
            )
        }
    }
}

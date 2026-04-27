package com.monkeys.projectmanager.utils

import com.monkeys.projectmanager.models.Task
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
@OptIn(ExperimentalUuidApi::class)
data class ProjectResponse(
    val projectName: String,
    val description: String
)

@Serializable
@OptIn(ExperimentalUuidApi::class)
data class ProjectFullResponse(
    var name: String,
    var description: String,
    var status: Int,
)

@Serializable
@OptIn(ExperimentalUuidApi::class)
data class ProjectDto(
    val projectUuid: Uuid,
    val userUuid: Uuid,
    val projectName: String,
    val description: String,
    val status: Int,
    val createdAt: String,
    val closedAt: String,
    val deletedAt: String
)

@Serializable
@OptIn(ExperimentalUuidApi::class)
data class TaskResponse(
    val title: String,
    val description: String,
    val wave: Int
)

@Serializable
@OptIn(ExperimentalUuidApi::class)
data class TaskFullResponse(
    val title: String,
    val description: String,
    val status: Int,
    val wave: Int,
    val blockedUntil: String
)

@Serializable
@OptIn(ExperimentalUuidApi::class)
data class TaskDto(
    val taskUuid: Uuid,
    val projectUuid: Uuid,
    val title: String,
    val description: String,
    val status: Int,
    val wave: Int,
    val createdAt: String,
    val blockedUntil: String,
    val completedAt: String,
    val deletedAt: String
)

@Serializable
@OptIn(ExperimentalUuidApi::class)
data class NoteResponse(
    val title: String,
    val content: String
)

@Serializable
@OptIn(ExperimentalUuidApi::class)
data class NotesDto (
    val noteUuid: Uuid,
    val title: String,
    val content: String,
    val createdAt: String,
    val deletedAt: String
)
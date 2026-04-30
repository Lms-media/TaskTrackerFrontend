package com.monkeys.projectmanager.utils

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
@OptIn(ExperimentalUuidApi::class)
data class ProjectFullResponse(
    var projectName: String,
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
    val createdAt: Long,
    val closedAt: Long? = null,
    val deletedAt: Long? = null
)

@Serializable
@OptIn(ExperimentalUuidApi::class)
data class TaskFullResponse(
    val title: String,
    val description: String,
    val status: Int,
    val wave: Int,
    val blockedUntilMs: Long?
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
    val createdAt: Long,
    val blockedUntil: Long? = null,
    val completedAt: Long? = null,
    val deletedAt: Long? = null
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
    val createdAt: Long,
    val deletedAt: Long? = null
)

@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val username: String? = null,
    val email: String? = null,
    val userId: String? = null,
    val accessTokenExpiresAt: Long? = null
)

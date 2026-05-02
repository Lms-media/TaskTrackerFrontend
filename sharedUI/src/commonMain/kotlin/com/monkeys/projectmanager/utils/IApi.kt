package com.monkeys.projectmanager.utils

import com.monkeys.projectmanager.models.Mark
import com.monkeys.projectmanager.models.Note
import com.monkeys.projectmanager.models.Project
import com.monkeys.projectmanager.models.Task
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
interface IApi {
    suspend fun getProjects(): List<Project>
    suspend fun getTasks(): List<Task>
    suspend fun getNotes(): List<Note>
    suspend fun getMarks(projectId: Uuid): List<Mark>

    suspend fun createProject(name: String, description: String): Uuid
    suspend fun getProject(id: Uuid): Project?
    suspend fun editProject(project: Project): Boolean
    suspend fun blockProject(id: Uuid, name: String, description: String, blockedUntil: Long): Uuid?
    suspend fun closeProject(id: Uuid): Boolean

    suspend fun createTask(
        projectId: Uuid,
        title: String,
        description: String,
        status: TaskStatus,
        wave: WaveStatus,
        blockedUntil: Long
    ): Uuid?

    suspend fun editTask(task: Task): Boolean
    suspend fun closeTask(id: Uuid): Boolean

    suspend fun createNote(title: String, text: String): Uuid
    suspend fun editNote(note: Note): Boolean
    suspend fun closeNote(id: Uuid): Boolean

    suspend fun createMark(projectId: Uuid, title: String, description: String): Uuid?
    suspend fun editMark(mark: Mark): Boolean
    suspend fun deleteMark(id: Uuid, projectId: Uuid): Boolean
}
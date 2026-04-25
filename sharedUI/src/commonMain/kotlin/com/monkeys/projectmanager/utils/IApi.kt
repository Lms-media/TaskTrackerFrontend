package com.monkeys.projectmanager.utils

import com.monkeys.projectmanager.models.Note
import com.monkeys.projectmanager.models.Project
import com.monkeys.projectmanager.models.Task
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
interface IApi {
    fun getProjects(): List<Project>
    fun getTasks(): List<Task>
    fun getNotes(): List<Note>

    fun createProject(name: String, description: String): Uuid
    fun getProject(id: Uuid): Project?
    fun editProject(project: Project): Boolean
    fun blockProject(id: Uuid, name: String, description: String, blockedUntil: Long): Uuid?
    fun closeProject(id: Uuid): Boolean

    fun createTask(projectId: Uuid, title: String, description: String, status: TaskStatus, wave: WaveStatus, blockedUntil: Long): Uuid?
    fun editTask(task: Task): Boolean
    fun closeTask(id: Uuid): Boolean

    fun createNote(title: String, text: String): Uuid
    fun editNote(note: Note): Boolean
    fun closeNote(id: Uuid): Boolean
}
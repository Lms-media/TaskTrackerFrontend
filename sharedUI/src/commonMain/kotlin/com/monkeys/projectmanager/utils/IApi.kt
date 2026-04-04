package com.monkeys.projectmanager.utils

import com.monkeys.projectmanager.models.Note
import com.monkeys.projectmanager.models.Project
import com.monkeys.projectmanager.models.Task
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
interface IApi {
    public fun getProjects(): List<Project>
    public fun getTasks(): List<Task>
    public fun getNotes(): List<Note>

    public fun createProject(name: String, description: String): Uuid
    public fun getProject(id: Uuid): Project?
    public fun editProject(project: Project): Boolean
    public fun blockProject(id: Uuid, blockedUntil: Long): Uuid?
    public fun closeProject(id: Uuid): Boolean

    public fun createTask(projectId: Uuid, title: String, description: String, status: Int, blockedUntil: Long): Uuid?
    public fun editTask(task: Task): Boolean
    public fun closeTask(id: Uuid): Boolean

    public fun createNote(title: String, text: String): Uuid
    public fun editNote(note: Note): Boolean
    public fun closeNote(id: Uuid): Boolean
}
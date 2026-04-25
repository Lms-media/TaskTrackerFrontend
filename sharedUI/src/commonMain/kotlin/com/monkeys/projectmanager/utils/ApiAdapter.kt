package com.monkeys.projectmanager.utils

import com.monkeys.projectmanager.models.Note
import com.monkeys.projectmanager.models.Project
import com.monkeys.projectmanager.models.Task
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
object ApiAdapter: IApi {
    private val api: IApi = LocalApi

    override fun getProjects(): List<Project> = api.getProjects()
    override fun getTasks(): List<Task> = api.getTasks()
    override fun getNotes(): List<Note> = api.getNotes()
    override fun createProject(name: String, description: String): Uuid {
        return api.createProject(name, description)
    }
    override fun getProject(id: Uuid): Project? {
        return api.getProject(id)
    }
    override fun editProject(project: Project): Boolean {
        return api.editProject(project)
    }
    override fun blockProject(id: Uuid, name: String, description: String, blockedUntil: Long): Uuid? {
        return api.blockProject(id, name, description, blockedUntil)
    }
    override fun closeProject(id: Uuid): Boolean {
        return api.closeProject(id)
    }
    override fun createTask(
        projectId: Uuid,
        title: String,
        description: String,
        status: TaskStatus,
        wave: WaveStatus,
        blockedUntil: Long
    ): Uuid? {
        return api.createTask(projectId, title, description, status, wave, blockedUntil)
    }
    override fun editTask(task: Task): Boolean {
        return api.editTask(task)
    }
    override fun closeTask(id: Uuid): Boolean {
        return api.closeTask(id)
    }
    override fun createNote(title: String, text: String): Uuid {
        return api.createNote(title, text)
    }
    override fun editNote(note: Note): Boolean {
        return api.editNote(note)
    }
    override fun closeNote(id: Uuid): Boolean {
        return api.closeNote(id)
    }
}
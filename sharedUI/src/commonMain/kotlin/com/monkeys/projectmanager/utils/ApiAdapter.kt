package com.monkeys.projectmanager.utils

import com.monkeys.projectmanager.models.Note
import com.monkeys.projectmanager.models.Project
import com.monkeys.projectmanager.models.Task
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
object ApiAdapter: IApi {
    private val api: IApi = HttpApi

    override suspend fun getProjects(): List<Project> = runCatching {
        api.getProjects()
    }.getOrElse {
        println("getProjects failed: ${it.message}")
        emptyList()
    }

    override suspend fun getTasks(): List<Task> = runCatching {
        api.getTasks()
    }.getOrElse {
        println("getTasks failed: ${it.message}")
        emptyList()
    }

    override suspend fun getNotes(): List<Note> = runCatching {
        api.getNotes()
    }.getOrElse {
        println("getNotes failed: ${it.message}")
        emptyList()
    }

    override suspend fun createProject(name: String, description: String): Uuid {
        return api.createProject(name, description)
    }

    override suspend fun getProject(id: Uuid): Project? {
        return runCatching {
            api.getProject(id)
        }.getOrElse {
            println("getProject failed: ${it.message}")
            null
        }
    }

    override suspend fun editProject(project: Project): Boolean {
        return runCatching {
            api.editProject(project)
        }.getOrElse {
            println("editProject failed: ${it.message}")
            false
        }
    }

    override suspend fun blockProject(id: Uuid, name: String, description: String, blockedUntil: Long): Uuid? {
        return runCatching {
            api.blockProject(id, name, description, blockedUntil)
        }.getOrElse {
            println("blockProject failed: ${it.message}")
            null
        }
    }

    override suspend fun closeProject(id: Uuid): Boolean {
        return runCatching {
            api.closeProject(id)
        }.getOrElse {
            println("closeProject failed: ${it.message}")
            false
        }
    }

    override suspend fun createTask(
        projectId: Uuid,
        title: String,
        description: String,
        status: TaskStatus,
        wave: WaveStatus,
        blockedUntil: Long
    ): Uuid? {
        return runCatching {
            api.createTask(projectId, title, description, status, wave, blockedUntil)
        }.getOrElse {
            println("createTask failed: ${it.message}")
            null
        }
    }

    override suspend fun editTask(task: Task): Boolean {
        return runCatching {
            api.editTask(task)
        }.getOrElse {
            println("editTask failed: ${it.message}")
            false
        }
    }

    override suspend fun closeTask(id: Uuid): Boolean {
        return runCatching {
            api.closeTask(id)
        }.getOrElse {
            println("closeTask failed: ${it.message}")
            false
        }
    }

    override suspend fun createNote(title: String, text: String): Uuid {
        return api.createNote(title, text)
    }

    override suspend fun editNote(note: Note): Boolean {
        return runCatching {
            api.editNote(note)
        }.getOrElse {
            println("editNote failed: ${it.message}")
            false
        }
    }

    override suspend fun closeNote(id: Uuid): Boolean {
        return runCatching {
            api.closeNote(id)
        }.getOrElse {
            println("closeNote failed: ${it.message}")
            false
        }
    }
}

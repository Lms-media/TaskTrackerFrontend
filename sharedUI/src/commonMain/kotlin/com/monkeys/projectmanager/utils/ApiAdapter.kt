package com.monkeys.projectmanager.utils

import com.monkeys.projectmanager.models.Mark
import com.monkeys.projectmanager.models.Note
import com.monkeys.projectmanager.models.Project
import com.monkeys.projectmanager.models.Task
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
object ApiAdapter: IApi {
    private val api: IApi = HttpApi

    private fun Throwable.isExpectedCancellation(): Boolean {
        val errorMessage = message.orEmpty()
        return errorMessage.contains("coroutine scope left the composition", ignoreCase = true) ||
                errorMessage.contains("Parent job is Cancelling", ignoreCase = true)
    }

    override suspend fun getProjects(): List<Project> = runCatching {
        api.getProjects()
    }.getOrElse {
        if (!it.isExpectedCancellation()) {
            println("getProjects failed: ${it.message}")
        }
        emptyList()
    }

    suspend fun getProjectHistory(): List<ProjectHistoryItem> = runCatching {
        when (api) {
            is HttpApi -> api.getProjectHistory()
            else -> api.getProjects().map {
                ProjectHistoryItem(
                    projectUuid = it.id,
                    createdAt = it.createdDate,
                    closedAt = null,
                    deletedAt = null
                )
            }
        }
    }.getOrElse {
        if (!it.isExpectedCancellation()) {
            println("getProjectHistory failed: ${it.message}")
        }
        emptyList()
    }

    override suspend fun getTasks(): List<Task> = runCatching {
        api.getTasks()
    }.getOrElse {
        if (!it.isExpectedCancellation()) {
            println("getTasks failed: ${it.message}")
        }
        emptyList()
    }

    override suspend fun getNotes(): List<Note> = runCatching {
        api.getNotes()
    }.getOrElse {
        if (!it.isExpectedCancellation()) {
            println("getNotes failed: ${it.message}")
        }
        emptyList()
    }

    override suspend fun getMarks(projectId: Uuid): List<Mark> = runCatching {
        api.getMarks(projectId)
    }.getOrElse {
        if (!it.isExpectedCancellation()) {
            println("getMarks failed: ${it.message}")
        }
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

    override suspend fun createMark(
        projectId: Uuid,
        title: String,
        description: String
    ): Uuid? {
        return api.createMark(projectId, title, description)
    }

    override suspend fun editMark(mark: Mark): Boolean {
        return runCatching {
            api.editMark(mark)
        }.getOrElse {
            println("editMark failed: ${it.message}")
            false
        }
    }

    override suspend fun deleteMark(id: Uuid, projectId: Uuid): Boolean {
        return runCatching {
            api.deleteMark(id, projectId)
        }.getOrElse {
            println("deleteMark failed: ${it.message}")
            false
        }
    }
}

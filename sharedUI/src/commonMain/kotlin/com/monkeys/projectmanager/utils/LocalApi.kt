package com.monkeys.projectmanager.utils

import androidx.compose.runtime.mutableStateListOf
import com.monkeys.projectmanager.models.Mark
import com.monkeys.projectmanager.models.Note
import com.monkeys.projectmanager.models.Project
import com.monkeys.projectmanager.models.Task
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
object LocalApi : IApi {
    private val projects = mutableStateListOf<Project>()
    private val tasks = mutableStateListOf<Task>()
    private val notes = mutableStateListOf<Note>()
    private val marks = mutableStateListOf<Mark>()

    override suspend fun getProjects(): List<Project> = projects
    override suspend fun getTasks(): List<Task> = tasks
    override suspend fun getNotes(): List<Note> = notes
    override suspend fun getMarks(projectId: Uuid): List<Mark> = marks

    override suspend fun createProject(name: String, description: String): Uuid {
        val projectId = Uuid.random()
        projects.add(
            Project(
                projectId,
                name,
                description,
                ProjectStatus.OFF,
                mutableStateListOf(),
                Clock.System.now().toEpochMilliseconds()
            )
        )
        return projectId
    }

    override suspend fun getProject(id: Uuid): Project? {
        return projects.find { it.id == id }?.let { return it }
    }

    override suspend fun editProject(project: Project): Boolean {
        val index = projects.indexOfFirst { it.id == project.id }
        return if (index != -1) {
            projects.removeAt(index)
            projects.add(index, project)
            true
        } else false
    }

    override suspend fun blockProject(id: Uuid, name: String, description: String, blockedUntil: Long): Uuid? {
        return createTask(id, name, description, TaskStatus.BLOCKED, WaveStatus.ACTIVE, blockedUntil)
    }

    override suspend fun closeProject(id: Uuid): Boolean {
        val index = projects.indexOfFirst { it.id == id }
        return if (index != -1) {
            projects[index].tasks.forEach { task ->
                task.status = TaskStatus.CLOSED
                editTask(task)
            }
            projects.removeAt(index)
            true
        } else false
    }

    override suspend fun createTask(
        projectId: Uuid,
        title: String,
        description: String,
        status: TaskStatus,
        wave: WaveStatus,
        blockedUntil: Long
    ): Uuid? {
        val index = projects.indexOfFirst { it.id == projectId }
        val project = projects[index]
        if (project.status == ProjectStatus.OFF) {
            val taskId = Uuid.random()
            val task = Task(
                taskId,
                projectId,
                title,
                description,
                status,
                wave,
                Clock.System.now().toEpochMilliseconds(),
                blockedUntil
            )
            tasks.add(task)

            project.tasks.add(task)
            project.status = ProjectStatus.ON
            projects.removeAt(index)
            projects.add(index, project)

            return taskId
        }
        return null
    }

    override suspend fun editTask(task: Task): Boolean {
        val index = tasks.indexOfFirst { it.id == task.id }
        return if (index != -1) {
            tasks.removeAt(index)
            tasks.add(index, task)
            true
        } else false
    }

    override suspend fun closeTask(id: Uuid): Boolean {
        val index = tasks.indexOfFirst { it.id == id }

        return if (index != -1) {
            val task = tasks[index]
            val projectNewStatus =
                if (task.status == TaskStatus.BLOCKED) ProjectStatus.OFF_FROM_BLOCK else ProjectStatus.OFF

            val projIndex = projects.indexOfFirst { it.id == task.projectId }
            val project = projects[projIndex]

            task.status = TaskStatus.CLOSED
            project.status = projectNewStatus

            val taskProjIndex = project.tasks.indexOfFirst { it.id == id }
            project.tasks.removeAt(taskProjIndex)
            project.tasks.add(taskProjIndex, task)

            projects.removeAt(projIndex)
            projects.add(projIndex, project)

            tasks.removeAt(index)
            tasks.add(index, task)
            true
        } else false
    }

    override suspend fun createNote(title: String, text: String): Uuid {
        val noteId = Uuid.random()
        notes.add(
            Note(
                noteId,
                title,
                text,
                Clock.System.now().toEpochMilliseconds()
            )
        )
        return noteId
    }

    override suspend fun editNote(note: Note): Boolean {
        val index = notes.indexOfFirst { it.id == note.id }
        return if (index != -1) {
            notes.removeAt(index)
            notes.add(index, note)
            true
        } else false
    }

    override suspend fun closeNote(id: Uuid): Boolean {
        val index = notes.indexOfFirst { it.id == id }
        return if (index != -1) {
            notes.removeAt(index)
            true
        } else false
    }

    override suspend fun createMark(
        projectId: Uuid,
        title: String,
        description: String
    ): Uuid? {
        TODO("Not yet implemented")
    }

    override suspend fun editMark(mark: Mark): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun deleteMark(id: Uuid, projectId: Uuid): Boolean {
        TODO("Not yet implemented")
    }
}
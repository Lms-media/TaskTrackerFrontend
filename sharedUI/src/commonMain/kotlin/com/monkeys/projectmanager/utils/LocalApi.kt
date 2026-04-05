package com.monkeys.projectmanager.utils

import androidx.compose.runtime.mutableStateListOf
import com.monkeys.projectmanager.models.Note
import com.monkeys.projectmanager.models.Project
import com.monkeys.projectmanager.models.Task
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
object LocalApi: IApi {
    private val projects = mutableStateListOf<Project>()
    private val tasks = mutableStateListOf<Task>()
    private val notes = mutableStateListOf<Note>()

    override fun getProjects(): List<Project> = projects
    override fun getTasks(): List<Task> = tasks
    override fun getNotes(): List<Note> = notes

    override fun createProject(name: String, description: String): Uuid {
        val projectId = Uuid.random()
        projects.add(
            Project(
                projectId,
                name,
                description,
                projectStatusOff,
                mutableStateListOf(),
                Clock.System.now().toEpochMilliseconds()
            )
        )
        return projectId
    }
    override fun getProject(id: Uuid): Project? {
        return projects.find { it.id == id }?.let { return it }
    }
    override fun editProject(project: Project): Boolean {
        val index = projects.indexOfFirst { it.id == project.id }
        return if (index != -1) {
            projects[index] = project
            true
        } else false
    }
    override fun blockProject(id: Uuid, blockedUntil: Long): Uuid? {
        return createTask(id, "block", "block", statusBlocked, blockedUntil)
    }
    override fun closeProject(id: Uuid): Boolean {
        val index = projects.indexOfFirst { it.id == id }
        return if (index != -1) {
            projects.removeAt(index)
            true
        } else false
    }

    override fun createTask(
        projectId: Uuid,
        title: String,
        description: String,
        status: Int,
        blockedUntil: Long
    ): Uuid? {
        val index = projects.indexOfFirst { it.id == projectId }
        val project = projects[index]
        if (project.status == projectStatusOff){
            val taskId = Uuid.random()
            val task = Task(
                taskId,
                projectId,
                title,
                description,
                status,
                Clock.System.now().toEpochMilliseconds(),
                blockedUntil
            )
            tasks.add(task)

            project.tasks.add(task)
            project.status = projectStatusOn
            projects.removeAt(index)
            projects.add(index, project)

            return taskId
        }
        return null
    }
    override fun editTask(task: Task): Boolean {
        val index = tasks.indexOfFirst { it.id == task.id }
        return if (index != -1) {
            tasks[index] = task
            true
        } else false
    }
    override fun closeTask(id: Uuid): Boolean {
        val index = tasks.indexOfFirst { it.id == id }

        return if (index != -1) {
            val task = tasks[index]

            val projIndex = projects.indexOfFirst { it.id == task.projectId }
            val project = projects[projIndex]

            task.status = statusClosed
            project.status = projectStatusOff

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

    override fun createNote(title: String, text: String): Uuid {
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
    override fun editNote(note: Note): Boolean {
        val index = notes.indexOfFirst { it.id == note.id }
        return if (index != -1) {
            notes[index] = note
            true
        } else false
    }
    override fun closeNote(id: Uuid): Boolean {
        val index = notes.indexOfFirst { it.id == id }
        return if (index != -1) {
            notes.removeAt(index)
            true
        } else false
    }
}
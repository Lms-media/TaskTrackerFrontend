package com.monkeys.projectmanager.utils

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.autofill.ContentType
import com.monkeys.projectmanager.models.Note
import com.monkeys.projectmanager.models.Project
import com.monkeys.projectmanager.models.Task
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.ContentType as KtorContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Instant


@OptIn(ExperimentalUuidApi::class)
object HttpApi : IApi {
    private val projects = mutableStateListOf<Project>()
    private val tasks = mutableStateListOf<Task>()
    private val notes = mutableStateListOf<Note>()

    private val serverUrl = "https://localhost:5273/"
    private val projectUrl = "/api/Projects"
    private val tasksUrl = "/api/Tasks"
    private val notesUrl = "/api/Notes"
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }
            )
        }
    }
    fun String.toMillis(): Long {
        return Instant.parse(this).toEpochMilliseconds()
    }

    fun Long.toDateTimeString(): String {
        return Instant.fromEpochMilliseconds(this).toString()
    }

    suspend fun updateProjectsAndTasks() {
        projects.clear()
        val responseProjects: List<ProjectDto> = client.get(serverUrl + projectUrl).body()
        responseProjects.forEach { dto ->
            val responseTasks: List<TaskDto> = client.get(tasksUrl + "/" + dto.projectUuid.toString()).body()
            val parsedTasks = responseTasks.map { tdto ->
                Task(
                    tdto.taskUuid,
                    tdto.projectUuid,
                    tdto.title,
                    tdto.description,
                    TaskStatus.entries.get(tdto.status),
                    WaveStatus.entries.get(tdto.wave),
                    tdto.createdAt.toMillis(),
                    tdto.blockedUntil.toMillis()
                )
            }
            projects.add(
                Project(
                    dto.projectUuid,
                    dto.projectName,
                    dto.description,
                    ProjectStatus.entries.get(dto.status),
                    parsedTasks.toMutableStateList(),
                    dto.createdAt.toMillis()
                )
            )
        }
    }

    suspend fun updateNotes() {
        notes.clear()
        val responseNotes: List<NotesDto> = client.get(serverUrl + notesUrl).body()
        responseNotes.forEach { dto ->
            val parsedNote = Note(
                dto.noteUuid,
                dto.title,
                dto.content,
                dto.createdAt.toMillis(),
            )
            notes.add(parsedNote)
        }
    }

    suspend fun updateData() {
        updateNotes()
        updateProjectsAndTasks()
    }
    override suspend fun getProjects(): List<Project> {
        updateProjectsAndTasks()
        return projects
    }
    override suspend fun getTasks(): List<Task> {
        updateProjectsAndTasks()
        return tasks
    }
    override suspend fun getNotes(): List<Note> {
        updateNotes()
        return notes
    }

    override suspend fun createProject(name: String, description: String): Uuid {
        val projectResponse = ProjectResponse(name, description)
        val responseProject: ProjectDto = client.post(serverUrl + projectUrl) {
            contentType(KtorContentType.Application.Json)
            setBody(projectResponse)
        }.body()
        val id = responseProject.projectUuid
        updateProjectsAndTasks()
        return id
    }

    override suspend fun getProject(id: Uuid): Project? {
        updateProjectsAndTasks()
        return projects.find { it.id == id }?.let { return it }
    }

    override suspend fun editProject(project: Project): Boolean {
        val index = projects.indexOfFirst { it.id == project.id }
        return if (index != -1) {
            val request = serverUrl + projectUrl + "/" + project.id.toString()
            val requestProject = ProjectFullResponse(
                project.name,
                project.description,
                project.status.ordinal
            )
            client.request(request) {
                contentType(KtorContentType.Application.Json)
                setBody(requestProject)
            }
            updateProjectsAndTasks()
            true
        } else false
    }

    override suspend fun blockProject(id: Uuid, name: String, description: String, blockedUntil: Long): Uuid? {
        return createTask(id, name, description, TaskStatus.BLOCKED, WaveStatus.ACTIVE, blockedUntil)
    }

    override suspend fun closeProject(id: Uuid): Boolean {
        val index = projects.indexOfFirst { it.id == id }
        return if (index != -1) {
            val request = serverUrl + projectUrl + id.toString() + "/close"
            client.request(request)
            updateProjectsAndTasks()
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
            val task = TaskFullResponse(
                title,
                description,
                status.ordinal,
                wave.ordinal,
                blockedUntil.toDateTimeString()
            )
            val request = serverUrl + "/" + projectId.toString() + "/tasks"
            val responseTask: TaskDto = client.post(request) {
                contentType(KtorContentType.Application.Json)
                setBody(task)
            }.body()
            val taskId = responseTask.taskUuid
            updateProjectsAndTasks()
            return taskId
        }
        return null
    }

    override suspend fun editTask(task: Task): Boolean {
        val index = tasks.indexOfFirst { it.id == task.id }
        return if (index != -1) {
            val requestTask = TaskFullResponse(
                task.title,
                task.description,
                task.status.ordinal,
                task.wave.ordinal,
                task.blockedUntil.toDateTimeString()
            )
            val request = serverUrl + tasksUrl + "/" + task.id.toString()
            client.request(request) {
                method = HttpMethod.Patch
                contentType(KtorContentType.Application.Json)
                setBody(requestTask)
            }
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
            val projectRequest = ProjectFullResponse(
                projects[projIndex].name,
                projects[projIndex].description,
                projectNewStatus.ordinal,
            )
            val requestProject = serverUrl + projectUrl + "/" + projects[projIndex].id.toString()
            client.request(requestProject) {
                method = HttpMethod.Patch
                contentType(KtorContentType.Application.Json)
                setBody(projectRequest)
            }

            task.status = TaskStatus.CLOSED
            val taskRequest = TaskFullResponse(
                tasks[index].title,
                tasks[index].description,
                TaskStatus.CLOSED.ordinal,
                tasks[index].wave.ordinal,
                tasks[index].blockedUntil.toDateTimeString()
            )
            val request = serverUrl + tasksUrl + "/" + task.id.toString()
            client.request(request) {
                method = HttpMethod.Patch
                contentType(KtorContentType.Application.Json)
                setBody(taskRequest)
            }
            updateProjectsAndTasks()
            true
        } else false
    }

    override suspend fun createNote(title: String, text: String): Uuid {
        val request = serverUrl + notesUrl
        val noteRequest = NoteResponse(
            title,
            text,
        )
        val outputNote: NotesDto = client.post(request) {
            contentType(KtorContentType.Application.Json)
            setBody(noteRequest)
        }.body()
        return outputNote.noteUuid
    }

    override suspend fun editNote(note: Note): Boolean {
        val index = notes.indexOfFirst { it.id == note.id }
        return if (index != -1) {
            val request = serverUrl + notesUrl + "/" + note.id.toString()
            val noteRequest = NoteResponse(
                note.title,
                note.text,
            )
            client.request(request) {
                method = HttpMethod.Patch
                contentType(KtorContentType.Application.Json)
                setBody(noteRequest)
            }
            true
        } else false
    }

    override suspend fun closeNote(id: Uuid): Boolean {
        val index = notes.indexOfFirst { it.id == id }
        return if (index != -1) {
            val request = serverUrl + notesUrl + "/" + id.toString()
            client.request(request) {
                method = HttpMethod.Delete
            }
            true
        } else false
    }
}

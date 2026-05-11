package com.monkeys.projectmanager.utils

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.toMutableStateList
import com.monkeys.projectmanager.models.Mark
import com.monkeys.projectmanager.models.Note
import com.monkeys.projectmanager.models.Project
import com.monkeys.projectmanager.models.Task
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.ContentType as KtorContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlin.time.Clock

@OptIn(ExperimentalUuidApi::class)
object HttpApi : IApi {
    private object BackendProjectStatus {
        const val CREATED = 0
        const val IN_WORK = 1
        const val CLOSED = 3
        const val DELETED = 4
        const val UNBLOCK = 5
    }

    private object BackendTaskStatus {
        const val CREATED = 0
        const val BLOCKED = 1
        const val IN_WORK = 2
        const val CLOSED = 4
    }

    private val projects = mutableStateListOf<Project>()
    private val tasks = mutableStateListOf<Task>()
    private val notes = mutableStateListOf<Note>()
    private val authUser = RegisterRequest(
        username = "frontend",
        email = "frontend@localhost.local",
        password = "Frontend123!"
    )
    private var accessToken: String? = null
    private var accessTokenExpiresAt: Long = 0L
    private val authMutex = Mutex()

    private val client = HttpClient {
        expectSuccess = true
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }
            )
        }
    }

    private fun Int.toFrontendProjectStatus(): ProjectStatus {
        return when (this) {
            BackendProjectStatus.CREATED -> ProjectStatus.OFF
            BackendProjectStatus.IN_WORK -> ProjectStatus.ON
            BackendProjectStatus.UNBLOCK -> ProjectStatus.OFF_FROM_BLOCK
            else -> ProjectStatus.ON
        }
    }

    private fun ProjectStatus.toBackendProjectStatus(): Int {
        return when (this) {
            ProjectStatus.ON -> BackendProjectStatus.IN_WORK
            ProjectStatus.OFF -> BackendProjectStatus.CREATED
            ProjectStatus.OFF_FROM_BLOCK -> BackendProjectStatus.UNBLOCK
        }
    }

    private fun Int.toFrontendTaskStatus(): TaskStatus {
        return when (this) {
            BackendTaskStatus.BLOCKED -> TaskStatus.BLOCKED
            BackendTaskStatus.IN_WORK -> TaskStatus.ACTIVE_CURRENT
            BackendTaskStatus.CLOSED -> TaskStatus.CLOSED
            else -> TaskStatus.ACTIVE
        }
    }

    private fun TaskStatus.toBackendTaskStatus(): Int {
        return when (this) {
            TaskStatus.ACTIVE -> BackendTaskStatus.CREATED
            TaskStatus.BLOCKED -> BackendTaskStatus.BLOCKED
            TaskStatus.ACTIVE_CURRENT -> BackendTaskStatus.IN_WORK
            TaskStatus.CLOSED -> BackendTaskStatus.CLOSED
        }
    }

    private fun apiUrl(path: String): String {
        return serverUrl.trimEnd('/') + "/" + path.trimStart('/')
    }

    private fun AuthResponse.bearerToken(): String {
        return this.accessToken ?: error("Auth response does not contain accessToken")
    }

    private fun isAccessTokenFresh(): Boolean {
        val refreshSkewMillis = 60_000L
        return accessToken != null &&
                accessTokenExpiresAt - refreshSkewMillis > Clock.System.now().toEpochMilliseconds()
    }

    private fun clearAuthorization() {
        accessToken = null
        accessTokenExpiresAt = 0L
    }

    private suspend fun login(): String {
        val response: AuthResponse = client.post(apiUrl(loginUrl)) {
            contentType(KtorContentType.Application.Json)
            setBody(
                LoginRequest(
                    username = authUser.username,
                    password = authUser.password
                )
            )
        }.body()

        accessTokenExpiresAt = response.accessTokenExpiresAt
            ?: (Clock.System.now().toEpochMilliseconds() + 10 * 60_000L)
        return response.bearerToken()
    }

    private suspend fun register() {
        client.post(apiUrl(registerUrl)) {
            contentType(KtorContentType.Application.Json)
            setBody(authUser)
        }
    }

    private suspend fun ensureAuthorized() {
        if (isAccessTokenFresh()) return

        authMutex.withLock {
            if (isAccessTokenFresh()) return

            accessToken = runCatching {
                login()
            }.getOrElse {
                register()
                login()
            }
        }
    }

    private suspend fun bearerHeader(): String {
        ensureAuthorized()
        return "Bearer ${accessToken.orEmpty()}"
    }

    private suspend fun <T> withAuthorizedRetry(block: suspend (String) -> T): T {
        return try {
            block(bearerHeader())
        } catch (error: ResponseException) {
            if (error.response.status != HttpStatusCode.Unauthorized) throw error
            clearAuthorization()
            block(bearerHeader())
        }
    }

    suspend fun updateProjectsAndTasks() {
        val now = Clock.System.now().toEpochMilliseconds()
        val responseProjects: List<ProjectDto> = withAuthorizedRetry { auth ->
            client.get(apiUrl(projectUrl)) {
                header(HttpHeaders.Authorization, auth)
            }.body()
        }
        val loadedProjects = responseProjects
            .filter { dto ->
                dto.status != BackendProjectStatus.CLOSED &&
                        dto.status != BackendProjectStatus.DELETED
            }
            .map { dto ->
            val responseTasks: List<TaskDto> = withAuthorizedRetry { auth ->
                client.get(apiUrl("api/projects/${dto.projectUuid}/tasks")) {
                    header(HttpHeaders.Authorization, auth)
                }.body()
            }
            val parsedTasks = responseTasks.map { responseTask ->
                Task(
                    responseTask.taskUuid,
                    responseTask.projectUuid,
                    responseTask.title,
                    responseTask.description,
                    responseTask.status.toFrontendTaskStatus(),
                    WaveStatus.entries.getOrElse(responseTask.wave) { WaveStatus.BACKLOG },
                    responseTask.createdAt,
                    responseTask.blockedUntil ?: 0L
                )
            }
            val projectStatus = if (parsedTasks.any {
                    it.status == TaskStatus.BLOCKED && it.blockedUntil > now
                }) {
                ProjectStatus.OFF_FROM_BLOCK
            } else {
                dto.status.toFrontendProjectStatus()
            }

            Project(
                dto.projectUuid,
                dto.projectName,
                dto.description,
                projectStatus,
                parsedTasks.toMutableStateList(),
                dto.createdAt
            )
        }

        projects.clear()
        tasks.clear()

        loadedProjects.forEach { project ->
            projects.add(project)
            tasks.addAll(project.tasks)
        }
    }

    suspend fun getProjectHistory(): List<ProjectHistoryItem> {
        val responseProjects: List<ProjectDto> = withAuthorizedRetry { auth ->
            client.get(apiUrl(projectUrl)) {
                header(HttpHeaders.Authorization, auth)
            }.body()
        }
        return responseProjects.map { dto ->
            ProjectHistoryItem(
                projectUuid = dto.projectUuid,
                createdAt = dto.createdAt,
                closedAt = dto.closedAt,
                deletedAt = dto.deletedAt
            )
        }
    }

    suspend fun updateNotes() {
        val responseNotes: List<NotesDto> = withAuthorizedRetry { auth ->
            client.get(apiUrl(notesUrl)) {
                header(HttpHeaders.Authorization, auth)
            }.body()
        }
        val loadedNotes = responseNotes.map { dto ->
            Note(
                dto.noteUuid,
                dto.title,
                dto.content,
                dto.createdAt,
            )
        }

        notes.clear()
        notes.addAll(loadedNotes)
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

    fun getMarksUrl(projectId: Uuid): String = "/api/projects/$projectId/marks"
    override suspend fun getMarks(projectId: Uuid): List<Mark> {
        val marks = mutableStateListOf<Mark>()
        val responseMarks: List<MarksDto> = withAuthorizedRetry { auth ->
            client.get(apiUrl(getMarksUrl(projectId))) {
                header(HttpHeaders.Authorization, auth)
            }.body()
        }
        val loadedNotes = responseMarks.map { dto ->
            Mark(
                dto.markUuid,
                dto.projectUuid,
                dto.title,
                dto.description,
                dto.createdAt,
            )
        }

        marks.addAll(loadedNotes)
        return marks
    }

    override suspend fun createProject(name: String, description: String): Uuid {
        val projectResponse = ProjectFullResponse(name, description, ProjectStatus.OFF.toBackendProjectStatus())
        val responseProject: ProjectDto = withAuthorizedRetry { auth ->
            client.post(apiUrl(projectUrl)) {
                header(HttpHeaders.Authorization, auth)
                contentType(KtorContentType.Application.Json)
                setBody(projectResponse)
            }.body()
        }
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
            val request = apiUrl("$projectUrl/${project.id}")
            val requestProject = ProjectFullResponse(
                project.name,
                project.description,
                project.status.toBackendProjectStatus()
            )
            withAuthorizedRetry { auth ->
                client.request(request) {
                    method = HttpMethod.Patch
                    header(HttpHeaders.Authorization, auth)
                    contentType(KtorContentType.Application.Json)
                    setBody(requestProject)
                }
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
            projects[index].tasks.forEach { task ->
                closeTask(task.id)
            }
            val request = apiUrl("$projectUrl/$id/close")
            withAuthorizedRetry { auth ->
                client.request(request) {
                    method = HttpMethod.Patch
                    header(HttpHeaders.Authorization, auth)
                }
            }
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
        updateProjectsAndTasks()
        val index = projects.indexOfFirst { it.id == projectId }
        if (index == -1) return null

        val project = projects[index]
        if (wave == WaveStatus.BACKLOG ||
            project.status == ProjectStatus.OFF ||
            project.status == ProjectStatus.OFF_FROM_BLOCK
        ) {
            val task = TaskFullResponse(
                title,
                description,
                status.toBackendTaskStatus(),
                wave.ordinal,
                blockedUntil.takeIf { status == TaskStatus.BLOCKED }
            )
            val request = apiUrl("api/projects/$projectId/tasks")
            val responseTask: TaskDto = withAuthorizedRetry { auth ->
                client.post(request) {
                    header(HttpHeaders.Authorization, auth)
                    contentType(KtorContentType.Application.Json)
                    setBody(task)
                }.body()
            }
            val taskId = responseTask.taskUuid
            if (wave != WaveStatus.BACKLOG) {
                val projectRequest = ProjectFullResponse(
                    project.name,
                    project.description,
                    if (status == TaskStatus.BLOCKED) {
                        ProjectStatus.OFF_FROM_BLOCK.toBackendProjectStatus()
                    } else {
                        ProjectStatus.ON.toBackendProjectStatus()
                    }
                )
                withAuthorizedRetry { auth ->
                    client.request(apiUrl("$projectUrl/$projectId")) {
                        method = HttpMethod.Patch
                        header(HttpHeaders.Authorization, auth)
                        contentType(KtorContentType.Application.Json)
                        setBody(projectRequest)
                    }
                }
            }
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
                task.status.toBackendTaskStatus(),
                task.wave.ordinal,
                task.blockedUntil.takeIf { task.status == TaskStatus.BLOCKED }
            )
            val request = apiUrl("$tasksUrl/${task.id}")
            withAuthorizedRetry { auth ->
                client.request(request) {
                    method = HttpMethod.Patch
                    header(HttpHeaders.Authorization, auth)
                    contentType(KtorContentType.Application.Json)
                    setBody(requestTask)
                }
            }
            updateProjectsAndTasks()
            true
        } else false
    }

    override suspend fun closeTask(id: Uuid): Boolean {
        val index = tasks.indexOfFirst { it.id == id }

        return if (index != -1) {
            val task = tasks[index]
            val projectNewStatus =
                if (
                    task.status == TaskStatus.BLOCKED &&
                    task.blockedUntil <= Clock.System.now().toEpochMilliseconds()
                ) {
                    ProjectStatus.OFF_FROM_BLOCK
                } else {
                    ProjectStatus.OFF
                }

            val projIndex = projects.indexOfFirst { it.id == task.projectId }
            val projectRequest = ProjectFullResponse(
                projects[projIndex].name,
                projects[projIndex].description,
                projectNewStatus.toBackendProjectStatus(),
            )
            val requestProject = apiUrl("$projectUrl/${projects[projIndex].id}")
            withAuthorizedRetry { auth ->
                client.request(requestProject) {
                    method = HttpMethod.Patch
                    header(HttpHeaders.Authorization, auth)
                    contentType(KtorContentType.Application.Json)
                    setBody(projectRequest)
                }
            }

            task.status = TaskStatus.CLOSED
            val request = apiUrl("$tasksUrl/${task.id}")
            withAuthorizedRetry { auth ->
                client.post("$request/close") {
                    header(HttpHeaders.Authorization, auth)
                }
            }
            updateProjectsAndTasks()
            true
        } else false
    }

    override suspend fun createNote(title: String, text: String): Uuid {
        val request = apiUrl(notesUrl)
        val noteRequest = NoteResponse(
            title,
            text,
        )
        val outputNote: NotesDto = withAuthorizedRetry { auth ->
            client.post(request) {
                header(HttpHeaders.Authorization, auth)
                contentType(KtorContentType.Application.Json)
                setBody(noteRequest)
            }.body()
        }
        updateNotes()
        return outputNote.noteUuid
    }

    override suspend fun editNote(note: Note): Boolean {
        val index = notes.indexOfFirst { it.id == note.id }
        return if (index != -1) {
            val request = apiUrl("$notesUrl/${note.id}")
            val noteRequest = NoteResponse(
                note.title,
                note.text,
            )
            withAuthorizedRetry { auth ->
                client.request(request) {
                    method = HttpMethod.Patch
                    header(HttpHeaders.Authorization, auth)
                    contentType(KtorContentType.Application.Json)
                    setBody(noteRequest)
                }
            }
            updateNotes()
            true
        } else false
    }

    override suspend fun closeNote(id: Uuid): Boolean {
        val index = notes.indexOfFirst { it.id == id }
        return if (index != -1) {
            val request = apiUrl("$notesUrl/$id")
            withAuthorizedRetry { auth ->
                client.request(request) {
                    method = HttpMethod.Delete
                    header(HttpHeaders.Authorization, auth)
                }
            }
            updateNotes()
            true
        } else false
    }

    override suspend fun createMark(
        projectId: Uuid,
        title: String,
        description: String
    ): Uuid {
        val request = apiUrl(getMarksUrl(projectId))
        val markRequest = MarkResponse(
            title,
            description,
        )
        val outputMark: MarksDto = withAuthorizedRetry { auth ->
            client.post(request) {
                header(HttpHeaders.Authorization, auth)
                contentType(KtorContentType.Application.Json)
                setBody(markRequest)
            }.body()
        }
        return outputMark.markUuid
    }

    override suspend fun editMark(mark: Mark): Boolean {
        val request = apiUrl("${getMarksUrl(mark.projectId)}/${mark.id}")
        val markRequest = MarkResponse(
            mark.title,
            mark.text,
        )
        val response = withAuthorizedRetry { auth ->
            client.request(request) {
                method = HttpMethod.Patch
                header(HttpHeaders.Authorization, auth)
                contentType(KtorContentType.Application.Json)
                setBody(markRequest)
            }
        }
        updateNotes()
        return response.status == HttpStatusCode.OK
    }

    override suspend fun deleteMark(id: Uuid, projectId: Uuid): Boolean {
        val request = apiUrl("${getMarksUrl(projectId)}/$id")
        val response = withAuthorizedRetry { auth ->
            client.request(request) {
                method = HttpMethod.Delete
                header(HttpHeaders.Authorization, auth)
            }
        }
        return response.status == HttpStatusCode.OK
    }
}

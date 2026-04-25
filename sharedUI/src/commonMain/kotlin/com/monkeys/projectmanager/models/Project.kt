package com.monkeys.projectmanager.models

import com.monkeys.projectmanager.utils.ProjectStatus
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class Project @OptIn(ExperimentalUuidApi::class) constructor(
    val id: Uuid,
    var name: String,
    var description: String,
    var status: ProjectStatus,
    var tasks: MutableList<Task>,
    val createdDate: Long
)
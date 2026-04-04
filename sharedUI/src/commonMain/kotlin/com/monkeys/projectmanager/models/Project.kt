package com.monkeys.projectmanager.models

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class Project @OptIn(ExperimentalUuidApi::class) constructor(
    val id: Uuid,
    var name: String,
    var description: String,
    var status: Int,
    var tasks: MutableList<Task>,
    val cratedDate: Long
)
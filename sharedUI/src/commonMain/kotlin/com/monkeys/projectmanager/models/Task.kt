package com.monkeys.projectmanager.models

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class Task @OptIn(ExperimentalUuidApi::class) constructor(
    val id: Uuid,
    val projectId: Uuid,
    var title: String,
    var description: String,
    var status: Int,
    val cratedDate: Long,
    var blockedUntil: Long
)
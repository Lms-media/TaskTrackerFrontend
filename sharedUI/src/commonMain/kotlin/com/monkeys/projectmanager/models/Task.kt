package com.monkeys.projectmanager.models

import com.monkeys.projectmanager.utils.TaskStatus
import com.monkeys.projectmanager.utils.WaveStatus
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class Task @OptIn(ExperimentalUuidApi::class) constructor(
    val id: Uuid,
    val projectId: Uuid,
    var title: String,
    var description: String,
    var status: TaskStatus,
    var wave: WaveStatus,
    val createdDate: Long,
    var blockedUntil: Long
)
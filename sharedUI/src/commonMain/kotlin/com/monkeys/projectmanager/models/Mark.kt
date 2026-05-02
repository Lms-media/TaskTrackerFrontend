package com.monkeys.projectmanager.models

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class Mark @OptIn(ExperimentalUuidApi::class) constructor(
    val id: Uuid,
    val projectId: Uuid,
    var title: String,
    var text: String,
    val createdDate: Long
)
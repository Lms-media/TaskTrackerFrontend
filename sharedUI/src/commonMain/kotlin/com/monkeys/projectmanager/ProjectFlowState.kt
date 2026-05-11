package com.monkeys.projectmanager

import com.monkeys.projectmanager.models.Project
import com.monkeys.projectmanager.utils.ProjectStatus
import com.monkeys.projectmanager.utils.TaskStatus
import com.monkeys.projectmanager.utils.WaveStatus

internal fun Project.hasActiveBlock(now: Long): Boolean {
    return tasks.any {
        it.status == TaskStatus.BLOCKED && it.blockedUntil > now
    }
}

internal fun Project.hasExpiredBlock(now: Long): Boolean {
    return tasks.any {
        it.status == TaskStatus.BLOCKED && it.blockedUntil <= now
    }
}

internal fun Project.hasSelectedWaveTask(): Boolean {
    return tasks.any {
        (it.status == TaskStatus.ACTIVE || it.status == TaskStatus.ACTIVE_CURRENT) &&
                (it.wave == WaveStatus.ACTIVE || it.wave == WaveStatus.WAITING)
    }
}

internal fun Project.needsTaskSelection(now: Long): Boolean {
    return !hasActiveBlock(now) && !hasSelectedWaveTask()
}

internal fun Project.isUnlockedFromBlock(now: Long): Boolean {
    return status == ProjectStatus.OFF_FROM_BLOCK &&
            !hasActiveBlock(now) &&
            hasExpiredBlock(now)
}

internal fun Project.blocksTaskReview(now: Long): Boolean {
    return needsTaskSelection(now) && !isUnlockedFromBlock(now)
}

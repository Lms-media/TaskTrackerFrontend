package com.monkeys.projectmanager.utils

enum class TaskStatus {
    ACTIVE,
    CLOSED,
    BLOCKED,
    ACTIVE_CURRENT
}
/* TaskStatus in HTTP API
* 0 - ACTIVE
* 1 - CLOSED
* 2 - BLOCKED
* 3 - ACTIVE_CURRENT
*/

enum class WaveStatus {
    ACTIVE,
    WAITING
}
/* WaveStatus in HTTP API
* 0 - ACTIVE
* 1 - WAITING
*/

enum class ProjectStatus {
    ON, OFF, OFF_FROM_BLOCK
}
/* ProjectStatus in HTTP API
* 0 - ON
* 1 - OFF
* 2 - OFF_FROM_BLOCK
*/

enum class ActionType {
    GET_TASK,
    CREATE_NOTE,
    EDIT_LAST,
    THINK,
    NOTES,
    PROJECTS,
    ENUM_END
}
/* ActionType in HTTP API
* 0 - GET_TASK
* 1 - CREATE_NOTE
* 2 - EDIT_LAST
* 3 - THINK
* 4 - NOTES
* 5 - PROJECTS
* -1 - ENUM_END
*/

const val timeZone = 3 * 60 * 60 * 1000
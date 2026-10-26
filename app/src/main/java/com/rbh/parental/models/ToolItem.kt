package com.rbh.parental.models

data class ToolItem(
    val title: String,
    val description: String,
    val iconRes: Int,
    val bgColor: Int,
    val action: ToolAction
)

enum class ToolAction {
    BLOCK_APPS, SCREEN_TIME, SCHEDULE, CALLS_LOG, REPORT, EMERGENCY
}

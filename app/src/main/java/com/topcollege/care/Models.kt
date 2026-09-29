package com.topcollege.care

data class LessonModel(
    val lesson: Int,
    val startedAt: String,
    val finishedAt: String,
    val subjectName: String,
    val teacherName: String,
    val roomName: String
)

data class SkincareDay(
    val title: String,
    val morningSteps: List<String>,
    val eveningSteps: List<String>,
    val note: String = ""
)

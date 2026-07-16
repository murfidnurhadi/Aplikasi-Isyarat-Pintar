package com.isyarat.pintar

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Question(
    val id: Int,
    val text: String,
    val correctAnswerImage: String,
    val options: List<String>
) : Parcelable

@Parcelize
data class Level(
    val id: Int,
    val name: String,
    val questions: List<Question>,
    var isUnlocked: Boolean = false,
    var isCompleted: Boolean = false,
    var score: Int = 0
) : Parcelable

data class HistoryRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val levelId: Int,
    val levelName: String = "",
    val score: Int,
    val timestamp: Long = System.currentTimeMillis()
)

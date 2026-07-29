package com.isyarat.pintar

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Question(
    val id: Int,
    val text: String,
    val correctAnswerImages: List<String>,
    val options: List<String>
) : Parcelable {
    // Helper to get a random correct image if multiple are available
    fun getCorrectAnswerImage(): String = correctAnswerImages.random()
}

@Parcelize
data class Level(
    val id: Int,
    val name: String,
    val questions: List<Question>,
    var isUnlocked: Boolean = false,
    var isCompleted: Boolean = false,
    var score: Int = 0,
    val icon: String = ""
) : Parcelable

@Parcelize
data class HistoryRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val levelId: Int,
    val levelName: String = "",
    val score: Int,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable

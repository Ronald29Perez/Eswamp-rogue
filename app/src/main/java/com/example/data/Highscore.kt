package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "highscores")
data class Highscore(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val playerName: String,
    val floorsCleared: Int,
    val score: Int,
    val date: Long = System.currentTimeMillis()
)

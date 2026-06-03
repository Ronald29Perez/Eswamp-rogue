package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HighscoreDao {
    @Query("SELECT * FROM highscores ORDER BY floorsCleared DESC, score DESC LIMIT 10")
    fun getTopHighscores(): Flow<List<Highscore>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighscore(highscore: Highscore)

    @Query("DELETE FROM highscores")
    suspend fun clearAllHighscores()
}

package com.example.data

import kotlinx.coroutines.flow.Flow

class HighscoreRepository(private val highscoreDao: HighscoreDao) {
    val topHighscores: Flow<List<Highscore>> = highscoreDao.getTopHighscores()

    suspend fun insertHighscore(highscore: Highscore) {
        highscoreDao.insertHighscore(highscore)
    }

    suspend fun clearAll() {
        highscoreDao.clearAllHighscores()
    }
}

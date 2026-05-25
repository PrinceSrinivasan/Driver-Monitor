package com.drowsiness.app.viewmodel

import android.app.Application
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.drowsiness.app.R
import com.drowsiness.app.db.AppDatabase
import com.drowsiness.app.db.HistoryEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DetectionState(
    val isRunning: Boolean = false,
    val status: String = "WAITING",
    val ear: Float = 0f,
    val alerts: Int = 0,
    val earHistory: List<Float> = emptyList(),
    val awakeCount: Int = 1,
    val drowsyCount: Int = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val dao = db.historyDao()

    private val _state = MutableStateFlow(DetectionState())
    val state: StateFlow<DetectionState> = _state.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var alarmPlaying = false
    private var frameCount = 0

    init {
        loadStats()
    }

    fun toggle() {
        val wasRunning = _state.value.isRunning
        _state.value = _state.value.copy(isRunning = !wasRunning)
        if (wasRunning) {
            stopAlarm()
            _state.value = _state.value.copy(status = "WAITING", ear = 0f)
        }
    }

    fun onFrameResult(ear: Float, status: String) {
        val current = _state.value
        if (!current.isRunning) return

        frameCount++
        val newHistory = (current.earHistory + ear).takeLast(30)
        val newAlerts = if (status == "DROWSY") current.alerts + 1 else current.alerts
        val newAwake = if (status == "AWAKE") current.awakeCount + 1 else current.awakeCount
        val newDrowsy = if (status == "DROWSY") current.drowsyCount + 1 else current.drowsyCount

        _state.value = current.copy(
            status = status,
            ear = ear,
            alerts = newAlerts,
            earHistory = newHistory,
            awakeCount = newAwake,
            drowsyCount = newDrowsy
        )

        when (status) {
            "DROWSY" -> startAlarm()
            else -> stopAlarm()
        }

        // Save to DB every 30 frames to avoid flooding
        if (status != "WAITING" && frameCount % 30 == 0) {
            saveHistory(ear, status)
        }
    }

    private fun saveHistory(ear: Float, status: String) {
        viewModelScope.launch {
            val now = Date()
            dao.insert(
                HistoryEntity(
                    date = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(now),
                    time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now),
                    ear = ear,
                    status = status
                )
            )
        }
    }

    fun getHistory(limit: Int, callback: (List<HistoryEntity>) -> Unit) {
        viewModelScope.launch {
            callback(dao.getLastN(limit))
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            val awake = dao.getAwakeCount()
            val drowsy = dao.getDrowsyCount()
            _state.value = _state.value.copy(
                awakeCount = maxOf(awake, 1),
                drowsyCount = drowsy
            )
        }
    }

    private fun startAlarm() {
        if (alarmPlaying) return
        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(getApplication(), R.raw.alarm)
                mediaPlayer?.isLooping = true
            }
            mediaPlayer?.start()
            alarmPlaying = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAlarm() {
        if (!alarmPlaying) return
        try {
            mediaPlayer?.pause()
            mediaPlayer?.seekTo(0)
            alarmPlaying = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}

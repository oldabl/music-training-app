package com.oliveapps.chordtrainer

import androidx.lifecycle.ViewModel
import com.oliveapps.chordtrainer.util.ChordManager
import com.oliveapps.chordtrainer.util.Metronome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class UiState(
    val bpm: Int = 80,
    val isRunning: Boolean = false,
    val beatNumber: Int = 0,
    val countUp: Boolean = false,
    val key: String = "",
    val currentChord: String = "",
    val nextChord: String = "",
)

class MainViewModel : ViewModel() {

    private val chordManager = ChordManager()
    private val metronome = Metronome()

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    fun setBpm(bpm: Int) {
        _uiState.value = _uiState.value.copy(bpm = bpm)
    }

    fun setKey(key: String) {
        chordManager.setKey(key)
        _uiState.value = _uiState.value.copy(key = key)
    }

    fun intervalMs(): Long = 60000L / uiState.value.bpm

    fun start() {
        metronome.reset()
        chordManager.reset()
        _uiState.value = _uiState.value.copy(
            isRunning = true,
            beatNumber = metronome.beatNumber,
            countUp = metronome.countUp,
            currentChord = chordManager.currentChord,
            nextChord = chordManager.nextChord
        )
    }

    fun stop() {
        _uiState.value = _uiState.value.copy(isRunning = false)
    }

    fun onBeat() {
        val state = _uiState.value

        if (metronome.beatNumber == 0) {
            if(!metronome.countUp) chordManager.goToNextChord()
            chordManager.findNextChord()
        }

        _uiState.value = state.copy(
            beatNumber = metronome.beatNumber,
            countUp = metronome.countUp,
            currentChord = chordManager.currentChord,
            nextChord = chordManager.nextChord
        )

        metronome.advanceBeat()
    }
}

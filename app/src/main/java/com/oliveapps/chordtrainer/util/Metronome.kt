package com.oliveapps.chordtrainer.util

class Metronome(
    var bpm: Int = 80
) {
    var isRunning = false
    var beatNumber = 0
    var countUp = false

    fun nextBeat() {
        beatNumber++
        beatNumber %= 4
        if (beatNumber == 0) countUp = false
    }

    fun intervalMs(): Long = 60000L / bpm

    fun start() {
        reset()
        isRunning = true
    }

    fun stop() {
        reset()
        isRunning = false
    }

    fun reset() {
        beatNumber = 0
        countUp = true
    }
}

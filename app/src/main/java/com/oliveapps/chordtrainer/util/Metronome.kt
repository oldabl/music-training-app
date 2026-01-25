package com.oliveapps.chordtrainer.util

class Metronome {
    var beatNumber = 0
        private set
    var countUp = true
        private set

    fun reset() {
        beatNumber = 0
        countUp = true
    }

    fun advanceBeat() {
        beatNumber = (beatNumber + 1) % 4
        if(beatNumber == 0) countUp = false
    }
}
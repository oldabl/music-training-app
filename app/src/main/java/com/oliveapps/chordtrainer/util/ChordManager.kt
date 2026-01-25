package com.oliveapps.chordtrainer.util

class ChordManager {

    private var currentKeyChords = Array<String>(7) { "" }
    var nextChord = ""
        private set
    var currentChord = ""
        private set

    fun reset() {
        nextChord = ""
        currentChord = ""
    }

    fun goToNextChord(): String {
        currentChord = nextChord
        return currentChord
    }

    fun findNextChord(): String {
        var next = ""
        var tries = 0
        do {
            next = getRandomKeyChord()
            tries++
        } while(next == currentChord && tries < 2)

        nextChord = next
        return nextChord
    }

    private fun getRandomKeyChord(): String = currentKeyChords.random()

    fun setKey(key: String) {
        // Find the right notes and make them double
        // So that we find all notes in key, regardless of which note we start at
        var notes = Music.ALL_NOTES_WITH_SHARPS
        if(Music.KEYS_WITH_FLATS.contains("|$key|"))
            notes = Music.ALL_NOTES_WITH_FLATS

        // Find the index of first note
        val allNotes = "$notes|$notes".split('|')
        val firstNoteIndex = allNotes.indexOf(key)

        // Now we make the list we need to find all the notes
        var sumIntervals = 0
        for(noteNumber in 0..<7) {
            val noteInKey = allNotes[firstNoteIndex+sumIntervals] + Music.MAJOR_KEY_CHORD_QUALITIES[noteNumber]
            currentKeyChords[noteNumber] = noteInKey.trim()
            sumIntervals += Music.MAJOR_KEY_SEMITONE_INTERVALS[noteNumber].toString().toInt()
        }
    }
}
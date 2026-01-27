package com.oliveapps.chordtrainer.util

class Music {
    companion object { // static element
        const val ALL_MUSIC_KEYS = "C|Db|D|Eb|E|F|Gb|G|Ab|A|Bb|B"
        const val MAJOR_KEY_SEMITONE_INTERVALS = "2212221"
        const val MAJOR_KEY_CHORD_QUALITIES = " mm  m°"
        // const val KEYS_WITH_SHARPS = "|G|D|A|E|B|"
        const val KEYS_WITH_FLATS = "|F|Bb|Eb|Ab|Db|Gb|"
        const val ALL_NOTES_WITH_FLATS = "C|Db|D|Eb|E|F|Gb|G|Ab|A|Bb|B"
        const val ALL_NOTES_WITH_SHARPS = "C|C#|D|D#|E|F|F#|G|G#|A|A#|B"

        // Returns a list of key from the proper note names (translated)
        // The List returned must have the same number of elements...
        // ... as the number of keys in ALL_MUSIC_KEYS
        fun getMusicKeys(notes: Map<String, String>): List<String> {
            val listOfKeys = mutableListOf<String>()

            val allKeys: List<String> = ALL_MUSIC_KEYS.split('|')
            for(key in allKeys) {
                var note = notes[""+key[0]]
                if(key.length > 1) note += key[1]
                listOfKeys.add(""+note)
            }

            return listOfKeys
        }

        fun getKey(position: Int): String {
            return ALL_MUSIC_KEYS.split('|')[position]
        }
    }
}
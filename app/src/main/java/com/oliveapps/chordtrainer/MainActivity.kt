package com.oliveapps.chordtrainer

import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import com.oliveapps.chordtrainer.util.*


class MainActivity : ComponentActivity() {

    // To handle the periodicity of the metronome
    private lateinit var handler: Handler
    private lateinit var tickRunnable: Runnable

    // To play the metronome tick
    private lateinit var soundPool: SoundPool

    // To manage the metronome attributes
    private lateinit var metronome: Metronome

    // To store the chords to play
    private var currentKeyChords = Array<String>(7) { "" }
    private var nextChord = ""
    private var currentChord = ""
    private var countSameChord = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Get all view elements
        val wholeLayout = findViewById<LinearLayout>(R.id.wholeLayout)
        val chordGroup = findViewById<LinearLayout>(R.id.chordGroup)
        val currentChordText = findViewById<TextView>(R.id.currentChordText)
        val nextChordText = findViewById<TextView>(R.id.nextChordText)
        val keySpinner = findViewById<Spinner>(R.id.keySpinner)
        val bpmText = findViewById<TextView>(R.id.bpmText)
        val bpmTitleText = findViewById<TextView>(R.id.bpmTitleText)
        val seekBar = findViewById<SeekBar>(R.id.bpmSeekBar)
        val startStopButton = findViewById<Button>(R.id.startStopButton)

        // Initialise metronome
        metronome = Metronome(resources.getInteger(R.integer.DEFAULT_BPM))

        // Initialise metronome looper thread
        handler = Handler(Looper.getMainLooper())

        // Initialise metronome tick player
        soundPool = SoundPool.Builder().setMaxStreams(1).build()
        val tick1Sound = soundPool.load(this, R.raw.tick_1, 1)
        val tick234Sound = soundPool.load(this, R.raw.tick_234, 1)

        // Initialise view elements
        seekBar.progress = metronome.bpm
        bpmText.text = metronome.bpm.toString()
        bpmText.setTextSize(TypedValue.COMPLEX_UNIT_SP, getBpmTextSize(metronome.bpm))
        bpmTitleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, getBpmTitleTextSize(metronome.bpm))
        startStopButton.setText(R.string.start_button)

        // Handle changing the metronome bpm
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                metronome.bpm = progress
                bpmText.text = metronome.bpm.toString()
                bpmText.setTextSize(TypedValue.COMPLEX_UNIT_SP, getBpmTextSize(metronome.bpm))
                bpmTitleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, getBpmTitleTextSize(metronome.bpm))
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // Handle the metronome
        tickRunnable = object : Runnable {
            override fun run() {
                if (metronome.isRunning) {

                    // Display background longer on first beat
                    var metronomeClickDelay = resources.getInteger(R.integer.METRONOME_CLICK_DELAY).toLong()

                    // If has been started just now
                    if(metronome.countUp) {
                        val countDown = 4 - metronome.beatNumber
                        currentChordText.text = countDown.toString()
                    }
                    // If first beat, set next chord and UI colours
                    if(metronome.beatNumber == 0) {
                        if(!metronome.countUp) {
                            currentChord = nextChord
                            currentChordText.text = currentChord
                        }
                        findNextChord()
                        nextChordText.text = nextChord
                        metronomeClickDelay *= resources.getInteger(R.integer.METRONOME_CLICK_MULTIPLY_FIRST_BEAT)
                        // Play metronome sound for first beat
                        soundPool.play(tick1Sound, 1f, 1f, 1, 0, 1f)
                    } else {
                        // Play metronome sound for 2nd 3rd and 4th beats
                        soundPool.play(tick234Sound, 1f, 1f, 1, 0, 1f)
                    }

                    // Show visual beat
                    currentChordText.setBackgroundColor(getColor(R.color.teal_200))
                    handler.postDelayed({
                        currentChordText.setBackgroundColor(getColor(android.R.color.transparent))
                    }, metronomeClickDelay)

                    // Plan the next beat
                    handler.postDelayed(this, metronome.intervalMs())

                    // Prepare next beat
                    metronome.nextBeat()
                }
            }
        }

        // Handle selection of key
        keySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val keySelected = resources.getStringArray(R.array.keys_array)[position]
                makeCurrentKeyChordList(keySelected)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Handle the start and stop buttons
        startStopButton.setOnClickListener {
            if (!metronome.isRunning) {
                // Start metronome
                startMetronome()
                // Remove key selector and show stop button
                keySpinner.visibility = View.GONE
                startStopButton.setText(R.string.stop_button);
                // Display chords to play
                chordGroup.visibility = View.VISIBLE
                // Keep screen on while running
                wholeLayout.keepScreenOn = true
            } else {
                // Stop metronome
                stopMetronome()
                // Remove chords to play
                chordGroup.visibility = View.GONE
                // Display key selector and show start button
                keySpinner.visibility = View.VISIBLE
                startStopButton.setText(R.string.start_button)
                // Allow screen to turn off when stopping
                wholeLayout.keepScreenOn = false
            }
        }
    }

    // Handle leaving application
    override fun onDestroy() {
        soundPool.release()
        super.onDestroy()
    }

    // Return a random chord in the key selected
    fun getRandomKeyChord(): String {
        val cn = (0..6).random()
        return currentKeyChords[cn]
    }

    // Will fabricate the chord list for the current key
    fun makeCurrentKeyChordList(key: String) {
        // Find the right notes and make them double
        // So that we find all notes in key, regardless of which note we start at
        var allNotesWithAlterations = Music.ALL_NOTES_WITH_SHARPS
        if(Music.KEYS_WITH_FLATS.contains("|$key|"))
            allNotesWithAlterations = Music.ALL_NOTES_WITH_FLATS
        val allNotesWithAlterationsTwice = "$allNotesWithAlterations|$allNotesWithAlterations"

        // Find the index of first note
        var allNotes = allNotesWithAlterationsTwice.split('|')
        val firstNoteIndex = allNotes.indexOf(key)

        // Now we make the list we need to find all the notes
        val only12NotesFromKey = allNotes.subList(firstNoteIndex, firstNoteIndex + 12)
        var sumIntervals = 0
        for(noteNumber in 0..<7) {
            val noteInKey = only12NotesFromKey[sumIntervals] + Music.MAJOR_KEY_CHORD_QUALITIES[noteNumber]
            currentKeyChords[noteNumber] = noteInKey.trim()
            sumIntervals += Music.MAJOR_KEY_SEMITONE_INTERVALS[noteNumber].toString().toInt()
        }
    }

    // Set everything to start metronome
    fun startMetronome() {
        metronome.start()
        nextChord = ""
        currentChord = ""
        handler.post(tickRunnable)
    }

    // Update all flags when metronome stops
    fun stopMetronome() {
        metronome.stop()
        handler.removeCallbacks(tickRunnable)
    }

    // Find the next chord from the key chords
    fun findNextChord() {
        nextChord = getRandomKeyChord()
        if(currentChord == nextChord) countSameChord++
        else countSameChord = 0
        if(countSameChord == 2) {
            while(nextChord == currentChord) {
                nextChord = getRandomKeyChord()
            }
        }
    }

    // Get BPM text size
    fun getBpmTextSize(bpm: Int): Float {
        return resources.getInteger(R.integer.MIN_BPM_TEXT_SIZE)+
                (bpm.toFloat()-resources.getInteger(R.integer.MIN_BPM))*
                (resources.getInteger(R.integer.MAX_BPM_TEXT_SIZE)-
                        resources.getInteger(R.integer.MIN_BPM_TEXT_SIZE))/
                resources.getInteger(R.integer.MAX_BPM)
    }
    fun getBpmTitleTextSize(bpm: Int): Float {
        return resources.getInteger(R.integer.MIN_BPM_TITLE_TEXT_SIZE)+
                (bpm.toFloat()-resources.getInteger(R.integer.MIN_BPM))*
                (resources.getInteger(R.integer.MAX_BPM_TITLE_TEXT_SIZE)-
                        resources.getInteger(R.integer.MIN_BPM_TITLE_TEXT_SIZE))/
                resources.getInteger(R.integer.MAX_BPM)
    }
}

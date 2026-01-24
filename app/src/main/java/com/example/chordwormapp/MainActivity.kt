package com.example.chordwormapp

import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import com.example.chordwormapp.music.Music

class MainActivity : ComponentActivity() {

    // To handle the periodicity of the metronome
    private lateinit var handler: Handler
    private lateinit var tickRunnable: Runnable

    // To play the metronome tick
    private lateinit var soundPool: SoundPool

    // To manage the metronome attributes
    private var bpm = 60
    private var isRunning = false
    private var beatNumber = 0
    private var countUp = false

    // To store the chords to play
    private var currentKeyChords = Array<String>(7) { "" }
    private var nextChord = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Get all view elements
        val chordGroup = findViewById<LinearLayout>(R.id.chordGroup)
        val currentChordText = findViewById<TextView>(R.id.currentChordText)
        val nextChordText = findViewById<TextView>(R.id.nextChordText)
        val chordSpinner = findViewById<Spinner>(R.id.chordSpinner)
        val bpmText = findViewById<TextView>(R.id.bpmText)
        val seekBar = findViewById<SeekBar>(R.id.bpmSeekBar)
        val startStopButton = findViewById<Button>(R.id.startStopButton)

        // Initialise metronome looper thread
        handler = Handler(Looper.getMainLooper())

        // Initialise metronome tick player
        soundPool = SoundPool.Builder().setMaxStreams(1).build()
        val tickSound = soundPool.load(this, R.raw.tick, 1)

        // Initialise view elements
        seekBar.progress = bpm
        bpmText.text = makeBpmText(bpm)
        startStopButton.setText(R.string.start_button)

        // Handle changing the metronome bpm
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                bpm = progress
                bpmText.text = makeBpmText(bpm)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // Handle the metronome
        tickRunnable = object : Runnable {
            override fun run() {
                if (isRunning) {

                    // If has been started just now
                    if(countUp) {
                        val countDown = 4 - beatNumber
                        currentChordText.text = countDown.toString()
                    }
                    // If first beat
                    if(beatNumber == 0) {
                        if(!countUp) currentChordText.text = nextChord
                        nextChord = getRandomKeyChord()
                        nextChordText.text = nextChord
                    }

                    // Show visual beat
                    chordGroup.setBackgroundColor(getColor(R.color.grey))
                    handler.postDelayed({
                        chordGroup.setBackgroundColor(getColor(android.R.color.transparent))
                    }, 100)

                    // Play metronome sound
                    soundPool.play(tickSound, 1f, 1f, 1, 0, 1f)

                    // Plan the next beat
                    val interval = 60000L / bpm
                    handler.postDelayed(this, interval)

                    // Increase beat number %4
                    beatNumber++
                    beatNumber %= 4

                    // If back on first beat, count up is finished
                    if(beatNumber == 0) countUp = false
                }
            }
        }

        // Handle selection of key
        chordSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val keySelected = resources.getStringArray(R.array.keys_array)[position]
                println(keySelected)
                makeCurrentKeyChordList(keySelected)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Handle the start and stop buttons
        startStopButton.setOnClickListener {
            if (!isRunning) {
                // Start metronome
                isRunning = true
                countUp = true
                beatNumber = 0
                nextChord = ""
                handler.post(tickRunnable)
                // Remove key selector and show stop button
                chordSpinner.visibility = View.GONE
                startStopButton.setText(R.string.stop_button);
                // Display chords to play
                chordGroup.visibility = View.VISIBLE
            } else {
                // Stop metronome
                isRunning = false
                handler.removeCallbacks(tickRunnable)
                // Remove chords to play
                chordGroup.visibility = View.GONE
                // Display key selector and show start button
                chordSpinner.visibility = View.VISIBLE
                startStopButton.setText(R.string.start_button);
            }
        }
    }

    // Handle leaving application
    override fun onDestroy() {
        soundPool.release()
        super.onDestroy()
    }

    // Create text that shows BPM properly
    fun makeBpmText(bpm: Int): String {
        return bpm.toString() + " " + getString(R.string.bpm_text_title)
    }

    // Return a random chord in the key selected
    fun getRandomKeyChord(): String {
        val cn = (0..6).random()
        return currentKeyChords[cn]
    }

    fun makeCurrentKeyChordList(key: String) {
        var ALL_NOTES_WITH_ALTERATIONS_TWICE = Music.ALL_NOTES_WITH_SHARPS + '|' + Music.ALL_NOTES_WITH_SHARPS
        if(Music.KEYS_WITH_FLATS.contains("|"+key+"|"))
            ALL_NOTES_WITH_ALTERATIONS_TWICE = Music.ALL_NOTES_WITH_FLATS + '|' + Music.ALL_NOTES_WITH_FLATS

        var all_notes = ALL_NOTES_WITH_ALTERATIONS_TWICE.split('|')
        val firstNoteIndex = all_notes.indexOf(key)
        all_notes = all_notes.subList(firstNoteIndex, firstNoteIndex + 12)
        var sumIntervals = 0
        for(noteNumber in 0..<7) {
            val noteInKey = all_notes[sumIntervals] + Music.MAJOR_KEY_CHORD_QUALITIES[noteNumber]
            currentKeyChords[noteNumber] = noteInKey.trim()
            sumIntervals += Music.MAJOR_KEY_SEMITONE_INTERVALS[noteNumber].toString().toInt()
        }
    }
}

package com.oliveapps.chordtrainer

import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    // Timing
    private lateinit var handler: Handler
    private lateinit var tickRunnable: Runnable

    // Sound
    private lateinit var soundPool: SoundPool
    private var tick1Sound = 0
    private var tick234Sound = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Views
        val wholeLayout = findViewById<LinearLayout>(R.id.wholeLayout)
        val chordGroup = findViewById<LinearLayout>(R.id.chordGroup)
        val currentChordText = findViewById<TextView>(R.id.currentChordText)
        val nextChordText = findViewById<TextView>(R.id.nextChordText)
        val bpmText = findViewById<TextView>(R.id.bpmText)
        val bpmTitleText = findViewById<TextView>(R.id.bpmTitleText)
        val seekBar = findViewById<SeekBar>(R.id.bpmSeekBar)
        val startStopButton = findViewById<Button>(R.id.startStopButton)
        val keySelectorLayout = findViewById<LinearLayout>(R.id.keySelectorLayout)
        // Key dropdown selector
        val keyDropdown = findViewById<AutoCompleteTextView>(R.id.keyDropdown)
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            viewModel.getMusicKeys(makeNoteList())
        )
        keyDropdown.setAdapter(adapter)
        keyDropdown.keyListener = null
        keyDropdown.showSoftInputOnFocus = false

        // Helpers
        handler = Handler(Looper.getMainLooper())
        viewModel.setBpm(resources.getInteger(R.integer.DEFAULT_BPM))

        // Sounds
        soundPool = SoundPool.Builder().setMaxStreams(1).build()
        tick1Sound = soundPool.load(this, R.raw.tick_1, 1)
        tick234Sound = soundPool.load(this, R.raw.tick_234, 1)

        // Pass ViewModel state to UI
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->

                seekBar.progress = state.bpm
                bpmText.text = state.bpm.toString()
                bpmText.setTextSize(
                    TypedValue.COMPLEX_UNIT_SP,
                    getBpmTextSize(state.bpm)
                )
                bpmTitleText.setTextSize(
                    TypedValue.COMPLEX_UNIT_SP,
                    getBpmTitleTextSize(state.bpm)
                )

                if (state.isRunning) {
                    chordGroup.visibility = View.VISIBLE
                    keySelectorLayout.visibility = View.GONE
                    startStopButton.setText(R.string.stop_button)
                    wholeLayout.keepScreenOn = true
                } else {
                    chordGroup.visibility = View.GONE
                    keySelectorLayout.visibility = View.VISIBLE
                    startStopButton.setText(R.string.start_button)
                    wholeLayout.keepScreenOn = false
                }

                if(state.key == "") {
                    startStopButton.isEnabled = false
                    startStopButton.setTextColor(getColor(R.color.textButtonDisabled))
                } else {
                    startStopButton.isEnabled = true
                    startStopButton.setTextColor(getColor(R.color.textButton))
                }

                currentChordText.text =
                    if (state.countUp) (4 - state.beatNumber).toString()
                    else state.currentChord

                nextChordText.text = state.nextChord
            }
        }

        // Pass UI changes to ViewModel
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                viewModel.setBpm(progress)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        keyDropdown.setOnItemClickListener { _, _, position, _ ->
            viewModel.setKey(viewModel.getKey(position))
        }

        startStopButton.setOnClickListener {
            if (!viewModel.uiState.value.isRunning) {
                viewModel.start()
                handler.post(tickRunnable)
            } else {
                viewModel.stop()
                handler.removeCallbacks(tickRunnable)
            }
        }

        // Metronome and chord loop
        tickRunnable = object : Runnable {
            override fun run() {
                if (viewModel.uiState.value.isRunning) {

                    // Signal new beat to ViewModel
                    viewModel.onBeat()

                    // Get duration for changing background color on tick
                    var clickDelay =
                        resources.getInteger(R.integer.METRONOME_CLICK_DURATION).toLong()

                    // Check which beat we're on to change sound and display
                    val beatNumber = viewModel.uiState.value.beatNumber
                    if (beatNumber == 0) {
                        clickDelay *= resources.getInteger(
                            R.integer.METRONOME_CLICK_DURATION_MULTIPLIER_FIRST_BEAT
                        )
                        soundPool.play(tick1Sound, 1f, 1f, 1, 0, 1f)
                    } else {
                        soundPool.play(tick234Sound, 1f, 1f, 1, 0, 1f)
                    }

                    // Switch background colour on tick
                    currentChordText.setBackgroundColor(getColor(R.color.backgroundCurrentChordTick))
                    handler.postDelayed({
                        currentChordText.setBackgroundColor(
                            getColor(android.R.color.transparent)
                        )
                    }, clickDelay)

                    // Plan next beat
                    handler.postDelayed(this, viewModel.intervalMs())
                }
            }
        }
    }

    // Other lifecycle handlers
    override fun onDestroy() {
        soundPool.release()
        super.onDestroy()
    }
    override fun onStop() {
        viewModel.stop()
        handler.removeCallbacks(tickRunnable)
        super.onStop()
    }

    // Make a List<String> from string notes
    private fun makeNoteList(): Map<String, String> {
        return mapOf<String, String>(
            "C" to resources.getString(R.string.notes_C),
            "D" to resources.getString(R.string.notes_D),
            "E" to resources.getString(R.string.notes_E),
            "F" to resources.getString(R.string.notes_F),
            "G" to resources.getString(R.string.notes_G),
            "A" to resources.getString(R.string.notes_A),
            "B" to resources.getString(R.string.notes_B),
        )
    }

    // UI helpers
    private fun getBpmTextSize(bpm: Int): Float =
        resources.getInteger(R.integer.MIN_BPM_TEXT_SIZE) +
                (bpm.toFloat() - resources.getInteger(R.integer.MIN_BPM)) *
                (resources.getInteger(R.integer.MAX_BPM_TEXT_SIZE) -
                        resources.getInteger(R.integer.MIN_BPM_TEXT_SIZE)) /
                resources.getInteger(R.integer.MAX_BPM)

    private fun getBpmTitleTextSize(bpm: Int): Float =
        resources.getInteger(R.integer.MIN_BPM_TITLE_TEXT_SIZE) +
                (bpm.toFloat() - resources.getInteger(R.integer.MIN_BPM)) *
                (resources.getInteger(R.integer.MAX_BPM_TITLE_TEXT_SIZE) -
                        resources.getInteger(R.integer.MIN_BPM_TITLE_TEXT_SIZE)) /
                resources.getInteger(R.integer.MAX_BPM)
}

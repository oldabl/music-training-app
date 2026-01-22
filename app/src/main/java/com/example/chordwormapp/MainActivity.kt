package com.example.chordwormapp

// import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {

    private lateinit var handler: Handler
    private lateinit var tickRunnable: Runnable
    // private lateinit var mediaPlayer: MediaPlayer

    private var bpm = 60
    private var isRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val chordText = findViewById<TextView>(R.id.chordText)
        val bpmText = findViewById<TextView>(R.id.bpmText)
        val seekBar = findViewById<SeekBar>(R.id.bpmSeekBar)
        val startStopButton = findViewById<Button>(R.id.startStopButton)

        handler = Handler(Looper.getMainLooper())
        // mediaPlayer = MediaPlayer.create(this, R.raw.tick)

        seekBar.progress = bpm
        bpmText.text = getString(R.string.bpm_text_init)
        startStopButton.text = getString(R.string.start_button)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                bpm = maxOf(30, progress)
                bpmText.text = getString(R.string.bpm_text_init)
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        tickRunnable = object : Runnable {
            override fun run() {
                if (isRunning) {
                    // mediaPlayer.start()
                    val interval = 60000L / bpm
                    handler.postDelayed(this, interval)
                }
            }
        }

        startStopButton.setOnClickListener {
            if (!isRunning) {
                isRunning = true
                handler.post(tickRunnable)
                startStopButton.setText(R.string.stop_button);
            } else {
                isRunning = false
                handler.removeCallbacks(tickRunnable)
                startStopButton.setText(R.string.start_button);
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // mediaPlayer.release()
    }
}

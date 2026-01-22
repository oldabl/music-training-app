package com.example.metronome

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var handler: Handler
    private lateinit var tickRunnable: Runnable
    private lateinit var mediaPlayer: MediaPlayer

    private var bpm = 60
    private var isRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bpmText = findViewById<TextView>(R.id.bpmText)
        val seekBar = findViewById<SeekBar>(R.id.bpmSeekBar)
        val startButton = findViewById<Button>(R.id.startButton)
        val stopButton = findViewById<Button>(R.id.stopButton)

        handler = Handler(Looper.getMainLooper())
        mediaPlayer = MediaPlayer.create(this, R.raw.tick)

        seekBar.progress = bpm
        bpmText.text = "BPM: $bpm"

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                bpm = maxOf(30, progress)
                bpmText.text = "BPM: $bpm"
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        tickRunnable = object : Runnable {
            override fun run() {
                if (isRunning) {
                    mediaPlayer.start()
                    val interval = 60000L / bpm
                    handler.postDelayed(this, interval)
                }
            }
        }

        startButton.setOnClickListener {
            if (!isRunning) {
                isRunning = true
                handler.post(tickRunnable)
            }
        }

        stopButton.setOnClickListener {
            isRunning = false
            handler.removeCallbacks(tickRunnable)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }
}

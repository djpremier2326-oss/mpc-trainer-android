package com.bytbeat.mpctrainer

import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var soundPool: SoundPool
    private val pads = Array(4) { arrayOfNulls<PadSample>(16) }
    private val padButtons = mutableListOf<Button>()
    private var bank = 0
    private var selectedPad = 0
    private lateinit var status: TextView
    private lateinit var bankLabel: TextView

    data class PadSample(val soundId: Int, val name: String, val uri: Uri)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        soundPool = SoundPool.Builder()
            .setMaxStreams(16)
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build())
            .build()
        setContentView(buildUi())
        refreshPads()
    }

    private fun buildUi(): View {
        val shell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 18, 18, 18)
            setBackgroundColor(Color.rgb(215, 209, 195))
        }

        val title = TextView(this).apply {
            text = "MPC TRAINER  •  RETRO"
            textSize = 20f
            setTextColor(Color.rgb(35, 35, 32))
            setTypeface(typeface, 1)
            setPadding(8, 6, 8, 14)
        }
        shell.addView(title)

        val screen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 14, 18, 14)
            setBackgroundColor(Color.rgb(55, 69, 62))
        }
        bankLabel = TextView(this).apply {
            textSize = 18f
            setTextColor(Color.rgb(206, 231, 192))
            setTypeface(typeface, 1)
        }
        status = TextView(this).apply {
            text = "NEW PROJECT  •  EMPTY PROGRAM\nTap a pad, then LOAD SAMPLE"
            textSize = 13f
            setTextColor(Color.rgb(206, 231, 192))
        }
        screen.addView(bankLabel)
        screen.addView(status)
        shell.addView(screen, LinearLayout.LayoutParams(-1, 112))

        val bankRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("A", "B", "C", "D").forEachIndexed { index, name ->
            bankRow.addView(Button(this).apply {
                text = "BANK $name"
                textSize = 11f
                setOnClickListener { bank = index; refreshPads() }
            }, LinearLayout.LayoutParams(0, 58, 1f))
        }
        shell.addView(bankRow)

        val grid = GridLayout(this).apply {
            columnCount = 4
            rowCount = 4
            setPadding(4, 10, 4, 10)
        }
        repeat(16) { i ->
            val b = Button(this).apply {
                textSize = 11f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.rgb(62, 61, 57))
                setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        selectedPad = i
                        playPad(i)
                        refreshPads()
                    }
                    true
                }
            }
            padButtons += b
            grid.addView(b, GridLayout.LayoutParams().apply {
                width = 0
                height = 118
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(5, 5, 5, 5)
            })
        }
        shell.addView(grid, LinearLayout.LayoutParams(-1, 0, 1f))

        val load = Button(this).apply {
            text = "LOAD SAMPLE TO SELECTED PAD"
            setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "audio/*"
                }
                startActivityForResult(intent, 900)
            }
        }
        shell.addView(load, LinearLayout.LayoutParams(-1, 62))

        val transport = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("REC", "OVERDUB", "STOP", "PLAY").forEach { label ->
            transport.addView(Button(this).apply {
                text = label
                isEnabled = label == "STOP" || label == "PLAY"
                setOnClickListener { status.text = "$label  •  sequencer engine next build" }
            }, LinearLayout.LayoutParams(0, 58, 1f))
        }
        shell.addView(transport)
        return shell
    }

    private fun playPad(index: Int) {
        val sample = pads[bank][index]
        if (sample == null) {
            status.text = "BANK ${'A' + bank}  •  PAD ${index + 1} EMPTY"
        } else {
            soundPool.play(sample.soundId, 1f, 1f, 1, 0, 1f)
            status.text = "PAD ${index + 1}  •  ${sample.name}"
        }
    }

    private fun refreshPads() {
        bankLabel.text = "BANK ${'A' + bank}  |  PAD ${selectedPad + 1}"
        padButtons.forEachIndexed { i, button ->
            val sample = pads[bank][i]
            button.text = if (sample == null) "${i + 1}\nEMPTY" else "${i + 1}\n${sample.name.take(10)}"
            button.alpha = if (i == selectedPad) 1f else .86f
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 900 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val fd = contentResolver.openFileDescriptor(uri, "r") ?: return
            val soundId = soundPool.load(fd.fileDescriptor, 0, fd.statSize, 1)
            fd.close()
            val name = displayName(uri)
            pads[bank][selectedPad] = PadSample(soundId, name, uri)
            status.text = "LOADED  •  $name  →  PAD ${selectedPad + 1}"
            refreshPads()
        }
    }

    private fun displayName(uri: Uri): String {
        contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) return c.getString(idx).substringBeforeLast('.')
        }
        return "SAMPLE"
    }

    override fun onDestroy() {
        soundPool.release()
        super.onDestroy()
    }
}

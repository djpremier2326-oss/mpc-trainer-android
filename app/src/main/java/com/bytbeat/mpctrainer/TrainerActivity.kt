package com.bytbeat.mpctrainer

import android.content.Intent
import android.graphics.Color
import android.media.*
import android.net.Uri
import android.os.*
import android.provider.OpenableColumns
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import kotlin.math.roundToLong
import org.json.JSONArray
import org.json.JSONObject

class TrainerActivity : AppCompatActivity() {
    data class PadSample(val soundId: Int, val name: String, val path: String)

    data class Event(
        val bank: Int,
        val pad: Int,
        val tick: Long,
        val track: Int,
        val velocity: Int,
        val tuneOffset: Int = 0,
    )

    private lateinit var pool: UnifiedAudioEngine
    private val readySounds = mutableSetOf<Int>()
    private val pendingAuditions = mutableMapOf<Int, Pair<Float, Float>>()
    private val pads = Array(4) { arrayOfNulls<PadSample>(16) }
    private val events = mutableListOf<Event>()
    private val recordedCycle = java.util.IdentityHashMap<Event, Long>()
    private val mute = BooleanArray(4)
    private val padButtons = mutableListOf<Button>()
    private val h = Handler(Looper.getMainLooper())
    private val tone by lazy { ToneGenerator(AudioManager.STREAM_MUSIC, 55) }
    private lateinit var status: TextView
    private lateinit var learnCard: TextView
    private lateinit var engine: LearnEngine
    private var displayWaveform: WaveformView? = null
    private var displayInfo: TextView? = null
    private var bank = 0
    private var pad = 0
    private var track = 0
    private var bpm = 90
    private var tc = 16
    private var swing = 50
    private var tcEnabled = true
    private var tcTriplet = false
    private var tcShift = 0
    private var playing = false
    private var recording = false
    private var overdub = false
    private var noteRepeat = false
    private var fullLevel = false
    private var levels16 = false
    private var levels16Type = "VELOCITY"
    private var levels16OriginalPad = 7
    private var sourceBank = 0
    private var sourcePad = 0
    private var startMs = 0L
    private var learn = false
    private var pending: Uri? = null
    private var pendingName = ""
    private var metronome = false
    private var countIn = true
    private var lastBeat = -1L
    private var lastClockTick = -1L
    private var countInToken = 0
    private var lastStopAt = -1000L
    private var lessonReady = false
    private var lessonDialog: AlertDialog? = null
    private var recordArmed = false
    private var pausedTick = 0L
    private var passRecorded = false
    private var passOverdub = false
    private var resettingProject = false
    private var lastUiAt = -1000L
    private var countingIn = false
    private val heldPads = linkedSetOf<Int>()
    private val history = SequenceHistory<Event>()
    private val importWorker = java.util.concurrent.Executors.newSingleThreadExecutor()

    private data class PadAssignment(
        val bank: Int,
        val pad: Int,
        val sample: PadSample,
        val action: String = "loaded",
    )

    private val pendingAssignments = mutableMapOf<Int, PadAssignment>()
    private val importingPads = mutableSetOf<Pair<Int, Int>>()
    private var workspaceMode = "MAIN"
    private var displayOverride: String? = null
    private var masterVolume = 1f
    private val padVolumes = Array(4) { FloatArray(16) { 1f } }
    private val padPitch = Array(4) { IntArray(16) { 0 } }
    private val browserPreview by lazy { BrowserPreview(this, pool) { masterVolume } }

    fun showPadVolumeOnDisplay(value: Int) {
        showDisplayMessage(levelReadout())
    }

    fun adjustSelectedPadPitch(delta: Int): Int {
        val old = padPitch[bank][pad]
        val v = (old + delta).coerceIn(-12, 12)
        padPitch[bank][pad] = v
        saveVolumes()
        if (v != old && pads[bank][pad] != null) report("pitch_changed")
        showDisplayMessage("PITCH  ${if(v>0)"+" else ""}$v st")
        return v
    }

    fun resetSelectedPadPitch() {
        padPitch[bank][pad] = 0
        saveVolumes()
        showDisplayMessage("PITCH  0 st")
    }

    fun setTimingCorrectEnabled(on: Boolean) {
        tcEnabled = on
        saveProject()
        showDisplayMessage("TC  ${if(on)"ON" else "OFF"} • 1/$tc • SWING $swing")
    }

    fun timingCorrectEnabled() = tcEnabled

    fun quantizeCurrentTrack(): Int {
        if (events.none { it.track == track }) {
            showDisplayMessage("TC DO IT • текущий TRACK пуст")
            return 0
        }
        history.checkpoint(events)
        var changed = 0
        for (i in events.indices) {
            val e = events[i]
            if (e.track == track) {
                val q = quant(e.tick)
                if (q != e.tick) {
                    val moved = e.copy(tick = q)
                    val born = recordedCycle.remove(e)
                    events[i] = moved
                    if (born != null) recordedCycle[moved] = born
                    changed++
                }
            }
        }
        saveProject()
        showDisplayMessage("TC DO IT • TRACK ${track+1} • $changed событий")
        report("tc")
        report("tc_do_it")
        return changed
    }

    private fun show16Levels() {
        if (!levels16) {
            sourceBank = bank
            sourcePad = pad
        }
        val types = arrayOf("VELOCITY", "TUNE")
        var selected = if (levels16Type == "TUNE") 1 else 0
        val box =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(densityDp(18), densityDp(8), densityDp(18), densityDp(4))
            }
        val active =
            CheckBox(this).apply {
                text = "ACTIVE"
                isChecked = levels16
            }
        val source =
            TextView(this).apply {
                text = "PAD   ${'A'+sourceBank}${(sourcePad+1).toString().padStart(2,'0')}"
                textSize = 16f
            }
        val choose = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL }
        types.forEachIndexed { i, name ->
            choose.addView(
                RadioButton(this).apply {
                    text = name
                    id = View.generateViewId()
                    isChecked = i == selected
                    setOnClickListener { selected = i }
                }
            )
        }
        val original =
            SeekBar(this).apply {
                max = 15
                progress = levels16OriginalPad
            }
        val originalLabel =
            TextView(this).apply { text = "ORIGINAL PAD   ${levels16OriginalPad+1}" }
        original.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(v: SeekBar?, p: Int, f: Boolean) {
                    originalLabel.text = "ORIGINAL PAD   ${p+1}"
                }

                override fun onStartTrackingTouch(v: SeekBar?) {}

                override fun onStopTrackingTouch(v: SeekBar?) {}
            }
        )
        box.addView(active)
        box.addView(source)
        box.addView(choose)
        box.addView(originalLabel)
        box.addView(original)
        box.addView(
            TextView(this).apply {
                text =
                    "VELOCITY: выбранный PAD на 16 уровнях громкости.\nTUNE: тот же PAD на 16 высотах; ORIGINAL PAD задаёт исходную высоту."
            }
        )
        AlertDialog.Builder(this)
            .setTitle("16 LEVELS")
            .setView(box)
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("DONE") { _, _ ->
                levels16 = active.isChecked
                levels16Type = types[selected]
                levels16OriginalPad = original.progress
                report("16_levels")
                showDisplayMessage("16 LEVELS ${if(levels16)"ON" else "OFF"} • $levels16Type")
                refreshPads()
                refreshStatus()
            }
            .show()
    }

    private fun showTimingCorrect() {
        val divisions = intArrayOf(4, 8, 16, 32, 64)
        var selected = divisions.indexOf(tc).let { if (it < 0) 2 else it }
        val box =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(densityDp(18), densityDp(8), densityDp(18), densityDp(4))
            }
        val enabled =
            CheckBox(this).apply {
                text = "GLOBAL TIMING CORRECT"
                isChecked = tcEnabled
            }
        val triplet =
            CheckBox(this).apply {
                text = "TRIPLET"
                isChecked = tcTriplet
            }
        val divLabel =
            TextView(this).apply {
                text = "TIME DIVISION   1/${divisions[selected]}${if(tcTriplet)"T" else ""}"
            }
        val div =
            SeekBar(this).apply {
                max = divisions.lastIndex
                progress = selected
            }
        val swingLabel = TextView(this).apply { text = "SWING   $swing%" }
        val sw =
            SeekBar(this).apply {
                max = 25
                progress = (swing - 50).coerceIn(0, 25)
            }
        val shiftLabel = TextView(this).apply { text = "SHIFT TIMING   $tcShift ticks" }
        val shift =
            SeekBar(this).apply {
                max = 192
                progress = tcShift + 96
            }
        fun refreshDiv() {
            divLabel.text =
                "TIME DIVISION   1/${divisions[selected]}${if(triplet.isChecked)"T" else ""}"
        }
        triplet.setOnCheckedChangeListener { _, _ -> refreshDiv() }
        div.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(v: SeekBar?, p: Int, f: Boolean) {
                    selected = p.coerceIn(0, divisions.lastIndex)
                    refreshDiv()
                }

                override fun onStartTrackingTouch(v: SeekBar?) {}

                override fun onStopTrackingTouch(v: SeekBar?) {}
            }
        )
        sw.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(v: SeekBar?, p: Int, f: Boolean) {
                    swingLabel.text = "SWING   ${50+p}%"
                }

                override fun onStartTrackingTouch(v: SeekBar?) {}

                override fun onStopTrackingTouch(v: SeekBar?) {}
            }
        )
        shift.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(v: SeekBar?, p: Int, f: Boolean) {
                    shiftLabel.text = "SHIFT TIMING   ${p-96} ticks"
                }

                override fun onStartTrackingTouch(v: SeekBar?) {}

                override fun onStopTrackingTouch(v: SeekBar?) {}
            }
        )
        box.addView(enabled)
        box.addView(divLabel)
        box.addView(div)
        box.addView(triplet)
        box.addView(swingLabel)
        box.addView(sw)
        box.addView(shiftLabel)
        box.addView(shift)
        box.addView(
            TextView(this).apply {
                text =
                    "TC выравнивает события по сетке. TRIPLET включает триольную сетку. SHIFT TIMING сдвигает результат вперёд или назад в ticks. DO IT применяет настройки к уже записанному текущему TRACK."
            }
        )
        AlertDialog.Builder(this)
            .setTitle("TIMING CORRECT")
            .setView(box)
            .setNegativeButton("CANCEL", null)
            .setNeutralButton("DO IT") { _, _ ->
                tc = divisions[selected]
                swing = 50 + sw.progress
                tcEnabled = enabled.isChecked
                tcTriplet = triplet.isChecked
                tcShift = shift.progress - 96
                saveProject()
                quantizeCurrentTrack()
                refreshStatus()
            }
            .setPositiveButton("DONE") { _, _ ->
                tc = divisions[selected]
                swing = 50 + sw.progress
                tcEnabled = enabled.isChecked
                tcTriplet = triplet.isChecked
                tcShift = shift.progress - 96
                saveProject()
                report("tc")
                showDisplayMessage(
                    "TC ${if(tcEnabled)"ON" else "OFF"} • 1/$tc${if(tcTriplet)"T" else ""} • SWING $swing • SHIFT $tcShift"
                )
                refreshStatus()
            }
            .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        volumeControlStream = AudioManager.STREAM_MUSIC
        pool = UnifiedAudioEngine(this)
        pool.setOnLoadCompleteListener { sp, id, statusCode ->
            runOnUiThread {
                if (isDestroyed || resettingProject) {
                    pool.unload(id)
                    return@runOnUiThread
                }
                if (statusCode == 0) {
                    readySounds.add(id)
                    pendingAssignments.remove(id)?.let { assignment ->
                        importingPads.remove(assignment.bank to assignment.pad)
                        unloadPadSound(pads[assignment.bank][assignment.pad])
                        pads[assignment.bank][assignment.pad] = assignment.sample
                        padVolumes[assignment.bank][assignment.pad] = 1f
                        padPitch[assignment.bank][assignment.pad] = 0
                        saveVolumes()
                        savePads()
                        refreshPads()
                        report(assignment.action)
                    }
                    pendingAuditions.remove(id)?.let { pr ->
                        try {
                            sp.play(id, pr.first, pr.first, 1, 0, pr.second)
                        } catch (_: RuntimeException) {
                            readySounds.remove(id)
                            pendingAuditions.remove(id)
                        }
                    }
                } else {
                    pendingAssignments.remove(id)?.let { assignment ->
                        importingPads.remove(assignment.bank to assignment.pad)
                        File(assignment.sample.path).delete()
                        pool.unload(id)
                    }
                    readySounds.remove(id)
                    pendingAuditions.remove(id)
                    Toast.makeText(
                            this,
                            "Не удалось загрузить аудио: файл слишком большой или формат не поддержан",
                            Toast.LENGTH_LONG,
                        )
                        .show()
                }
            }
        }
        restorePads()
        restoreProject()
        restoreVolumes()
        engine =
            LearnEngine(getSharedPreferences("mpc", MODE_PRIVATE).getInt("learn_engine_lesson", 0))
        engine.begin(snapshot())
        setContentView(buildUi())
        val resetPrefs = getSharedPreferences("learn", MODE_PRIVATE)
        if (resetPrefs.getBoolean("new_project_pending", false)) {
            resetPrefs.edit().remove("new_project_pending").apply()
            if (engine.lesson == LearnEngine.LAST_LESSON) {
                learn = true
                learnCard.visibility = View.VISIBLE
                val r = engine.report("new_project", snapshot())
                learnCard.text = "${engine.title()}\n${r.message}"
                saveLesson()
            }
        }
        refreshPads()
        refreshStatus()
        h.post { showCourseWelcomeIfNeeded() }
    }

    private fun densityDp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun btn(label: String, click: () -> Unit) =
        Button(this).apply {
            text = label
            textSize = 8.5f
            setOnClickListener { click() }
            if (label == "PLAY") {
                setOnLongClickListener {
                    playFromStart()
                    Toast.makeText(
                            this@TrainerActivity,
                            "PLAY START • с начала",
                            Toast.LENGTH_SHORT,
                        )
                        .show()
                    true
                }
            }
        }

    private fun row(vararg v: View) =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            v.forEach { addView(it, LinearLayout.LayoutParams(0, 48, 1f)) }
        }

    private fun buildUi(): View {
        val root =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(10, 8, 10, 8)
                setBackgroundColor(Color.rgb(216, 210, 195))
            }
        root.addView(
            TextView(this).apply {
                text = "MPC TRAINER • LIVE III BASICS"
                textSize = 16f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(Color.rgb(38, 37, 33))
            }
        )
        status =
            TextView(this).apply {
                setPadding(12, 8, 12, 8)
                textSize = 12f
                setTextColor(Color.rgb(207, 232, 193))
                setBackgroundColor(Color.rgb(53, 68, 60))
            }
        val displayFrame = FrameLayout(this)
        val statusScroll =
            ScrollView(this).apply {
                isFillViewport = true
                addView(status, FrameLayout.LayoutParams(-1, -2))
            }
        displayFrame.addView(statusScroll, FrameLayout.LayoutParams(-1, -1))
        displayWaveform = WaveformView(this).apply { visibility = View.GONE }
        displayFrame.addView(displayWaveform, FrameLayout.LayoutParams(-1, -1))
        displayInfo =
            TextView(this).apply {
                setTextColor(Color.rgb(235, 238, 222))
                textSize = 10f
                visibility = View.GONE
                gravity = Gravity.BOTTOM or Gravity.START
                setPadding(10, 0, 10, 6)
            }
        displayFrame.addView(displayInfo, FrameLayout.LayoutParams(-1, -1))
        root.addView(displayFrame, LinearLayout.LayoutParams(-1, 190))
        learnCard =
            TextView(this).apply {
                setPadding(10, 8, 10, 8)
                textSize = 11f
                setTextColor(Color.rgb(38, 37, 33))
                setBackgroundColor(Color.rgb(235, 226, 201))
                visibility = View.GONE
            }
        learnCard.setOnClickListener { showLessonExplanation() }
        root.addView(learnCard)
        root.addView(
            row(
                btn("MAIN") {
                    setWorkspaceMode("MAIN")
                    report("main")
                },
                btn("BROWSE") {
                    setWorkspaceMode("BROWSE")
                    report("browse")
                    browse()
                },
                btn("SAMPLE EDIT") {
                    setWorkspaceMode("SAMPLE EDIT")
                    sampleEdit()
                },
                btn("LEARN") {
                    learn = !learn
                    learnCard.visibility = if (learn) View.VISIBLE else View.GONE
                    if (learn) {
                        engine.begin(snapshot())
                        showLesson()
                    }
                },
            )
        )
        root.addView(
            row(
                *Array(4) { i ->
                    btn("BANK ${'A'+i}") {
                        stopRepeat()
                        bank = i
                        refreshPads()
                        refreshStatus()
                    }
                }
            )
        )
        val grid =
            GridLayout(this).apply {
                columnCount = 4
                rowCount = 4
            }
        repeat(16) { i ->
            val b =
                Button(this).apply {
                    textSize = 8.5f
                    gravity = Gravity.CENTER
                    setTextColor(Color.WHITE)
                    setBackgroundColor(Color.rgb(58, 57, 53))
                    setOnTouchListener { _, e ->
                        when (e.actionMasked) {
                            MotionEvent.ACTION_DOWN,
                            MotionEvent.ACTION_POINTER_DOWN -> {
                                parent.requestDisallowInterceptTouchEvent(true)
                                heldPads.add(i)
                                pad = i
                                hit(i, true)
                                report("pad")
                                if (noteRepeat) startRepeat()
                            }
                            MotionEvent.ACTION_UP,
                            MotionEvent.ACTION_CANCEL -> {
                                heldPads.remove(i)
                                if (heldPads.isEmpty()) {
                                    stopRepeat()
                                    parent.requestDisallowInterceptTouchEvent(false)
                                }
                            }
                        }
                        refreshPads()
                        true
                    }
                }
            padButtons += b
            grid.addView(
                b,
                GridLayout.LayoutParams().apply {
                    width = 0
                    height = 80
                    columnSpec = GridLayout.spec(i % 4, 1f)
                    rowSpec = GridLayout.spec(3 - i / 4)
                    setMargins(3, 3, 3, 3)
                },
            )
        }
        root.addView(grid, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(
            row(
                btn("NOTE REPEAT") {
                    displayOverride = null
                    noteRepeat = !noteRepeat
                    if (!noteRepeat) stopRepeat() else if (heldPads.isNotEmpty()) startRepeat()
                    report("note_repeat")
                    refreshStatus()
                },
                btn("FULL LEVEL") {
                    displayOverride = null
                    fullLevel = !fullLevel
                    report("full_level")
                    refreshStatus()
                },
                btn("16 LEVELS") { show16Levels() },
                btn("STEP SEQ") {
                    setWorkspaceMode("STEP SEQ")
                    steps()
                },
            )
        )
        root.addView(
            row(
                btn("TC") { showTimingCorrect() },
                btn("SWING") {
                    displayOverride = null
                    swing =
                        when (swing) {
                            50 -> 54
                            54 -> 58
                            else -> 50
                        }
                    saveProject()
                    report("swing")
                    refreshStatus()
                },
                btn("TRACK −") {
                    displayOverride = null
                    track = (track + 3) % 4
                    report("track")
                    refreshStatus()
                },
                btn("TRACK +") {
                    displayOverride = null
                    track = (track + 1) % 4
                    report("track")
                    refreshStatus()
                },
            )
        )
        root.addView(
            row(
                btn("METRO") {
                    displayOverride = null
                    metronome = !metronome
                    saveProject()
                    refreshStatus()
                },
                btn("COUNT IN") {
                    displayOverride = null
                    countIn = !countIn
                    saveProject()
                    refreshStatus()
                },
                btn("TRACK MUTE") {
                    displayOverride = null
                    mute[track] = !mute[track]
                    saveProject()
                    report("track_mute")
                    refreshStatus()
                },
                btn("SAVE") {
                    saveProject()
                    savePads()
                    report("save")
                    Toast.makeText(this, "Проект сохранён", Toast.LENGTH_SHORT).show()
                },
            )
        )
        root.addView(
            row(
                btn("REC") {
                    record(false)
                    report("record")
                },
                btn("OVERDUB") {
                    record(true)
                    report("overdub")
                },
                btn("STOP") { stopTransport() },
                btn("PLAY") { play() },
            )
        )
        root.addView(
            row(
                btn("RESTART COURSE") {
                    engine.reset(snapshot())
                    saveLesson()
                    learn = true
                    learnCard.visibility = View.VISIBLE
                    showLesson()
                },
                btn("CONTROL GUIDE") { showControlGuide() },
            )
        )
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars =
                insets.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.systemBars() or
                        androidx.core.view.WindowInsetsCompat.Type.displayCutout()
                )
            view.setPadding(
                densityDp(8) + bars.left,
                bars.top,
                densityDp(8) + bars.right,
                bars.bottom,
            )
            insets
        }
        androidx.core.view.ViewCompat.requestApplyInsets(root)
        return root
    }

    private fun snapshot() =
        LearnEngine.Snapshot(
            pads.sumOf { a -> a.count { it != null } },
            events.size,
            track,
            mute.count { it },
            tc,
            swing,
            noteRepeat,
            fullLevel,
            levels16,
        )

    private fun report(a: String) {
        if (!learn) return
        if (!lessonReady) {
            renderLessonCard()
            return
        }
        val r = engine.report(a, snapshot())
        if (r.advanced) {
            saveLesson()
            lessonReady = false
            stopRepeat()
            Toast.makeText(this, "Практика выполнена. Разберём следующий урок", Toast.LENGTH_SHORT)
                .show()
            renderLessonCard()
            if (!playing) h.post { showLessonExplanation() }
        } else {
            learnCard.text = "${engine.title()}\n${r.message}\nНажми карточку: объяснение и пример"
        }
    }

    private fun showLesson() {
        renderLessonCard()
        showLessonExplanation()
    }

    private fun saveLesson() {
        getSharedPreferences("mpc", MODE_PRIVATE)
            .edit()
            .putInt("learn_engine_lesson", engine.lesson)
            .apply()
    }

    private fun showControlGuide() {
        val n = MpcBasics.controls.map { it.name }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("CONTROL GUIDE")
            .setItems(n) { _, i ->
                val c = MpcBasics.controls[i]
                AlertDialog.Builder(this)
                    .setTitle(c.name)
                    .setMessage("${c.purpose}\n\nPRACTICE • ${c.practice}")
                    .setPositiveButton("OK", null)
                    .show()
            }
            .show()
    }

    private fun showCourseWelcomeIfNeeded() {
        if (isFinishing || isDestroyed) return
        val prefs = getSharedPreferences("mpc", MODE_PRIVATE)
        if (prefs.getInt("course_welcome", 0) >= 228) return
        prefs.edit().putInt("course_welcome", 228).apply()
        engine.selectLesson(0, snapshot())
        saveLesson()
        learn = true
        lessonReady = false
        learnCard.visibility = View.VISIBLE
        showLesson()
    }

    private fun renderLessonCard() {
        learnCard.text =
            "${engine.title()}\n${if(lessonReady)engine.prompt() else "ЗАДАНИЕ • Сначала разбери урок: нажми эту карточку"}\nНажми: объяснение • звуковой пример"
        refreshStatus()
    }

    private fun showLessonExplanation() {
        if (isFinishing || isDestroyed) return
        lessonDialog?.dismiss()
        val content = LessonContent.get(engine.lesson)
        val column =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(densityDp(20), densityDp(8), densityDp(20), densityDp(12))
            }
        fun section(title: String, body: String) {
            column.addView(
                TextView(this).apply {
                    text = title
                    textSize = 17f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setPadding(0, densityDp(14), 0, densityDp(5))
                }
            )
            column.addView(
                TextView(this).apply {
                    text = body
                    textSize = 16f
                    setLineSpacing(densityDp(3).toFloat(), 1f)
                }
            )
        }
        section("Что это", content.meaning)
        section("Зачем это нужно", content.purpose)
        section("Что услышать в примере", content.listen)
        column.addView(
            Button(this).apply {
                text = "▶ Послушать пример · 90 BPM"
                setOnClickListener { playTeachingExample(engine.lesson) }
            }
        )
        section("Практика", content.practice)
        val scroll = ScrollView(this).apply { addView(column) }
        lessonDialog =
            AlertDialog.Builder(this)
                .setTitle("${engine.lesson+1}/${LessonContent.lessons.size} · ${content.title}")
                .setView(scroll)
                .setPositiveButton(
                    if (lessonReady) "Вернуться к практике" else "Проверить понимание"
                ) { _, _ ->
                    browserPreview.stop()
                    if (lessonReady) renderLessonCard() else showUnderstandingCheck()
                }
                .setNeutralButton("Все уроки / звуки") { _, _ ->
                    browserPreview.stop()
                    showCourseMenu()
                }
                .setNegativeButton("Закрыть") { _, _ -> browserPreview.stop() }
                .create()
        lessonDialog?.setOnDismissListener { browserPreview.stop() }
        lessonDialog?.show()
    }

    private fun showUnderstandingCheck() {
        val lesson = engine.lesson
        val content = LessonContent.get(lesson)
        AlertDialog.Builder(this)
            .setTitle(content.check)
            .setItems(content.answers.toTypedArray()) { _, answer ->
                val right = answer == content.correct
                AlertDialog.Builder(this)
                    .setTitle(if (right) "Верно" else "Разберёмся")
                    .setMessage(content.feedback)
                    .setPositiveButton(if (right) "К практике" else "Попробовать ещё") { _, _ ->
                        if (right && engine.lesson == lesson) {
                            lessonReady = true
                            renderLessonCard()
                        } else showUnderstandingCheck()
                    }
                    .setNegativeButton("Объяснение") { _, _ -> showLessonExplanation() }
                    .show()
            }
            .setNegativeButton("Назад") { _, _ -> showLessonExplanation() }
            .show()
    }

    private fun showCourseMenu() {
        val items =
            (listOf("Добавить учебные звуки в свободные пэды") +
                    LessonContent.lessons.mapIndexed { i, l -> "${i+1}. ${l.title}" })
                .toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Курс MPC")
            .setItems(items) { _, i ->
                if (i == 0) {
                    addTeachingKit()
                    showLessonExplanation()
                } else {
                    if (playing || recordArmed) {
                        Toast.makeText(this, "Сначала останови транспорт", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        engine.selectLesson(i - 1, snapshot())
                        saveLesson()
                        lessonReady = false
                        learn = true
                        learnCard.visibility = View.VISIBLE
                        showLesson()
                    }
                }
            }
            .setNegativeButton("Закрыть", null)
            .show()
    }

    private fun playTeachingExample(index: Int) {
        if (playing || recordArmed) {
            Toast.makeText(
                    this,
                    "Сначала STOP: пример звучит отдельно от проекта",
                    Toast.LENGTH_LONG,
                )
                .show()
            return
        }
        try {
            val example = TrainingAudio.example(index, File(cacheDir, "teaching"))
            browserPreview.play(
                Uri.fromFile(example),
                onError = { msg -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show() },
            )
        } catch (e: Exception) {
            Toast.makeText(this, "Не удалось включить пример: ${e.message}", Toast.LENGTH_LONG)
                .show()
        }
    }

    fun loadPracticeKit() {
        addTeachingKit()
    }

    private fun addTeachingKit() {
        if (playing || recordArmed) {
            Toast.makeText(this, "Сначала STOP", Toast.LENGTH_SHORT).show()
            return
        }
        val target =
            (listOf(bank) + (0..3).filter { it != bank }).firstOrNull { b ->
                pads[b].count { it == null } >= 4
            }
        if (target == null) {
            Toast.makeText(
                    this,
                    "Для набора нужны 4 свободных пэда в одном банке. Существующие звуки не заменены",
                    Toast.LENGTH_LONG,
                )
                .show()
            return
        }
        try {
            val free = (0..15).filter { pads[target][it] == null }.take(4)
            val kit = TrainingAudio.kit(File(filesDir, "training_kit"))
            kit.forEachIndexed { i, item ->
                val slot = free[i]
                pads[target][slot] =
                    PadSample(
                        pool.load(item.second.absolutePath, 1),
                        item.first,
                        item.second.absolutePath,
                    )
                padVolumes[target][slot] = 1f
                padPitch[target][slot] = 0
            }
            bank = target
            pad = free.first()
            savePads()
            saveVolumes()
            saveProject()
            refreshPads()
            Toast.makeText(
                    this,
                    "Учебные звуки: BANK ${'A'+bank}, пэды ${free.joinToString{(it+1).toString()}}",
                    Toast.LENGTH_LONG,
                )
                .show()
        } catch (e: Exception) {
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun levelReadout(): String =
        "MASTER ${(masterVolume*100).roundToLong()}% • PAD ${(padVolumes[bank][pad]*100).roundToLong()}% • PITCH ${padPitch[bank][pad]} st${if(levels16)" • 16 LEVELS $levels16Type" else ""}"

    fun restoreNormalLevels() {
        masterVolume = 1f
        padVolumes[bank][pad] = 1f
        levels16 = false
        fullLevel = false
        noteRepeat = false
        stopRepeat()
        saveVolumes()
        refreshPads()
        showDisplayMessage(
            "Обычная громкость: MASTER 100% / PAD 100%\nPITCH сохранён: ${padPitch[bank][pad]} st"
        )
    }

    private fun showLevelHelp() {
        AlertDialog.Builder(this)
            .setTitle("Громкость звука")
            .setMessage(
                "${levelReadout()}\n\nMASTER управляет всем проектом. PAD — выбранным звуком. Если оба уровня уменьшены, их ослабление складывается. 16 LEVELS VELOCITY намеренно даёт разную силу ударов.\nКнопки громкости телефона теперь управляют именно мультимедиа."
            )
            .setPositiveButton("MASTER / PAD 100%") { _, _ -> restoreNormalLevels() }
            .setNegativeButton("Закрыть", null)
            .show()
    }

    private fun velocity(i: Int) =
        if (levels16 && levels16Type == "VELOCITY")
            (((i + 1) * 127f / 16f).roundToLong().toInt()).coerceIn(1, 127)
        else if (fullLevel) 127 else 100

    private fun levels16TuneSemitones(i: Int) = (i - levels16OriginalPad).coerceIn(-15, 15)

    private fun levels16Rate(i: Int) =
        java.lang.Math.pow(2.0, levels16TuneSemitones(i) / 12.0).toFloat().coerceIn(.5f, 2f)

    private fun effectiveVelocity(v: Int) = if (levels16 && levels16Type == "VELOCITY") v else 127

    private fun padGain(v: Int, b: Int, p: Int) =
        TrainerPlayback.gain(effectiveVelocity(v), padVolumes[b][p], masterVolume)

    private fun pitchRate(b: Int, p: Int) =
        java.lang.Math.pow(2.0, padPitch[b][p] / 12.0).toFloat().coerceIn(.5f, 2f)

    private fun playLoadedSound(soundId: Int, gain: Float, queueManual: Boolean, rate: Float = 1f) {
        if (soundId <= 0) return
        val g = gain.coerceIn(0f, 1f)
        val r = rate.coerceIn(.125f, 8f)
        try {
            if (readySounds.contains(soundId)) {
                pool.play(soundId, g, g, 1, 0, r)
            } else if (queueManual) {
                pendingAuditions[soundId] = Pair(g, r)
            }
        } catch (_: IllegalStateException) {
            readySounds.remove(soundId)
            pendingAuditions.remove(soundId)
        } catch (_: RuntimeException) {
            readySounds.remove(soundId)
            pendingAuditions.remove(soundId)
        }
    }

    private fun unloadPadSound(sample: PadSample?) {
        sample ?: return
        pendingAuditions.remove(sample.soundId)
        readySounds.remove(sample.soundId)
        pool.unload(sample.soundId)
    }

    private fun voiceParameters(e: Event) =
        PadVoice.parameters(
            e.velocity,
            padVolumes[e.bank][e.pad],
            masterVolume,
            padPitch[e.bank][e.pad],
            e.tuneOffset,
        )

    private fun playEvent(e: Event, manual: Boolean) {
        TrainerUiState.pulsePad(e.bank, e.pad)
        val params = voiceParameters(e)
        pads[e.bank][e.pad]?.let { playLoadedSound(it.soundId, params.gain, manual, params.rate) }
    }

    private fun hit(i: Int, user: Boolean) {
        if (user) displayOverride = null
        val b = if (levels16) sourceBank else bank
        val p = if (levels16) sourcePad else i
        val loaded = pads[b][p] != null
        val v = effectiveVelocity(velocity(i))
        val offset = if (levels16 && levels16Type == "TUNE") levels16TuneSemitones(i) else 0
        val event =
            Event(
                b,
                p,
                if (playing) {
                    if (tcEnabled) quant(tick()) else tick()
                } else 0L,
                track,
                v,
                offset,
            )
        playEvent(event, true)
        if (user && loaded && recording && playing) {
            events += event
            recordedCycle[event] = loopCycle()
            if (overdub) passOverdub = true else passRecorded = true
            saveProject()
        }
        if (user && loaded) {
            if (fullLevel) report("full_level_pad")
            if (levels16) report("16_levels_pad")
        }
        if (user)
            showDisplayMessage(
                "BANK ${'A'+b} • PAD ${p+1} • ${pads[b][p]?.name?:"EMPTY"}\n${levelReadout()}"
            )
        else refreshStatus()
    }

    private fun loopTicks() = 768L

    private fun absoluteTick() =
        if (!playing) pausedTick
        else ((SystemClock.elapsedRealtime() - startMs) / (60000.0 / bpm / 96.0)).toLong()

    private fun tick() = absoluteTick() % loopTicks()

    private fun loopCycle() = absoluteTick() / loopTicks()

    private fun tcGridTicks(): Long {
        val straight = (384L / tc.coerceAtLeast(1)).coerceAtLeast(1L)
        return if (tcTriplet) ((straight * 2L) / 3L).coerceAtLeast(1L) else straight
    }

    private fun quant(t: Long): Long {
        val g = tcGridTicks()
        var q = ((t + g / 2) / g) * g
        if (!tcTriplet && swing > 50 && ((q / g) % 2L) == 1L)
            q += ((swing - 50) / 25.0 * g / 2.0).roundToLong()
        q += tcShift
        return ((q % loopTicks()) + loopTicks()) % loopTicks()
    }

    private fun crossed(prev: Long, now: Long, target: Long) =
        TrainerPlayback.crossed(prev, now, target)

    private val clock =
        object : Runnable {
            override fun run() {
                if (!playing) return
                val t = tick()
                if (recording && !overdub && loopCycle() > 0) overdub = true
                val beat = t / 96
                if (metronome && beat != lastBeat) {
                    lastBeat = beat
                    tone.startTone(
                        if (beat % 4 == 0L) ToneGenerator.TONE_PROP_BEEP2
                        else ToneGenerator.TONE_PROP_BEEP,
                        45,
                    )
                }
                if (t != lastClockTick) {
                    val cycle = loopCycle()
                    events
                        .filter { ev ->
                            !mute[ev.track] &&
                                crossed(lastClockTick, t, ev.tick) &&
                                (recordedCycle[ev]?.let { born -> cycle > born } ?: true)
                        }
                        .forEach { e -> playEvent(e, false) }
                }
                lastClockTick = t
                val now = SystemClock.elapsedRealtime()
                if (now - lastUiAt >= 50) {
                    lastUiAt = now
                    refreshStatus()
                }
                h.postDelayed(this, 4)
            }
        }

    private fun startClock() {
        lastBeat = -1
        lastClockTick = if (pausedTick == 0L) -1L else pausedTick
        recordedCycle.clear()
        h.removeCallbacks(clock)
        h.post(clock)
    }

    private fun clearCurrentTrackForRecord() {
        events.removeAll { it.track == track }
        recordedCycle.keys.removeAll { it.track == track }
    }

    private fun record(od: Boolean) {
        displayOverride = null
        if (playing) {
            if (od) {
                overdub = !overdub
                recording = overdub
                recordArmed = false
                refreshStatus()
            } else showDisplayMessage("REC • сначала STOP, затем REC → PLAY")
            return
        }
        countInToken++
        countingIn = false
        recordArmed = true
        overdub = od
        recording = false
        passRecorded = false
        passOverdub = false
        showDisplayMessage(if (od) "OVERDUB готов • нажми PLAY" else "REC готов • нажми PLAY")
    }

    private fun countDown(beats: Int, token: Int, done: () -> Unit) {
        var left = beats
        val gap = (60000L / bpm).coerceAtLeast(100)
        val r =
            object : Runnable {
                override fun run() {
                    if (token != countInToken) return
                    if (left <= 0) {
                        done()
                        return
                    }
                    status.text = "COUNT IN • $left"
                    tone.startTone(
                        if (left == beats) ToneGenerator.TONE_PROP_BEEP2
                        else ToneGenerator.TONE_PROP_BEEP,
                        55,
                    )
                    left--
                    h.postDelayed(this, gap)
                }
            }
        h.post(r)
    }

    private fun play() {
        if (playing || countingIn) return
        displayOverride = null
        countInToken++
        val armed = recordArmed
        val token = countInToken
        val begin = {
            countingIn = false
            if (armed) history.checkpoint(events)
            if (armed && !overdub) clearCurrentTrackForRecord()
            recordArmed = false
            recording = armed
            playing = true
            startMs =
                SystemClock.elapsedRealtime() -
                    kotlin.math.ceil(pausedTick * (60000.0 / bpm / 96.0)).toLong()
            startClock()
            saveProject()
            refreshStatus()
        }
        if (armed && countIn) {
            countingIn = true
            countDown(4, token, begin)
        } else begin()
    }

    fun playFromStart() {
        countInToken++
        countingIn = false
        pool.stopAll()
        playing = false
        recording = false
        h.removeCallbacks(clock)
        stopRepeat()
        pausedTick = 0L
        play()
    }

    fun undoSequence(redo: Boolean = false): Boolean {
        if (playing || recordArmed || countingIn) {
            showDisplayMessage("Сначала STOP, затем ${if(redo)"REDO" else "UNDO"}")
            return false
        }
        val previous = if (redo) history.redo(events) else history.undo(events)
        if (previous == null) {
            showDisplayMessage("${if(redo)"REDO" else "UNDO"} • изменений нет")
            return false
        }
        events.clear()
        events.addAll(previous)
        recordedCycle.clear()
        saveProject()
        refreshStatus()
        showDisplayMessage("${if(redo)"REDO" else "UNDO"} • ${events.size} нот")
        return true
    }

    private fun stopTransport() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastStopAt < 500L) pool.stopAll()
        lastStopAt = now
        displayOverride = null
        countInToken++
        countingIn = false
        if (playing) pausedTick = tick()
        val didRecord = passRecorded
        val didOverdub = passOverdub
        playing = false
        recording = false
        recordArmed = false
        overdub = false
        lastClockTick = -1
        h.removeCallbacks(clock)
        pendingAuditions.clear()
        stopRepeat()
        if (didRecord) report("recorded_pass")
        if (didOverdub) report("overdub_pass")
        report(if (didOverdub && !didRecord) "overdub_stop" else "stop")
        passRecorded = false
        passOverdub = false
        saveProject()
        refreshStatus()
    }

    private fun noteRepeatIntervalMs() =
        (60000.0 / bpm * (tcGridTicks() / 96.0)).toLong().coerceAtLeast(4)

    private val repeater =
        object : Runnable {
            override fun run() {
                if (noteRepeat) {
                    heldPads.toList().forEach { held -> hit(held, true) }
                    val repeatBank = if (levels16) sourceBank else bank
                    val repeatPad = if (levels16) sourcePad else pad
                    if (pads[repeatBank][repeatPad] != null) report("note_repeat_pad")
                    h.postDelayed(this, noteRepeatIntervalMs())
                }
            }
        }

    private fun startRepeat() {
        if (heldPads.isEmpty()) heldPads.add(pad)
        h.removeCallbacks(repeater)
        h.postDelayed(repeater, noteRepeatIntervalMs())
    }

    private fun stopRepeat() {
        heldPads.clear()
        h.removeCallbacks(repeater)
    }

    private fun steps() {
        var page = 0
        val outer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val label =
            TextView(this).apply {
                gravity = Gravity.CENTER
                textSize = 13f
            }
        val box = GridLayout(this).apply { columnCount = 4 }
        val bs = ArrayList<Button>()
        repeat(16) {
            val b = Button(this)
            bs += b
            box.addView(
                b,
                GridLayout.LayoutParams().apply {
                    width =
                        (resources.displayMetrics.widthPixels -
                            (64 * resources.displayMetrics.density).toInt()) / 4
                    height = (56 * resources.displayMetrics.density).toInt()
                },
            )
        }
        outer.addView(label)
        outer.addView(box)
        val nav =
            row(
                btn("◀ BAR") {
                    page = (page + 1) % 2
                    redrawSteps(page, label, bs)
                },
                btn("BAR ▶") {
                    page = (page + 1) % 2
                    redrawSteps(page, label, bs)
                },
            )
        outer.addView(nav)
        redrawSteps(page, label, bs)
        AlertDialog.Builder(this)
            .setTitle("STEP SEQUENCER")
            .setView(outer)
            .setPositiveButton("DONE") { _, _ -> refreshStatus() }
            .setOnCancelListener { refreshStatus() }
            .show()
    }

    private fun redrawSteps(page: Int, label: TextView, bs: List<Button>) {
        label.text = "BAR ${page+1} • TRACK ${track+1} • BANK ${'A'+bank} • PAD ${pad+1} • 1/16"
        bs.forEachIndexed { i, b ->
            val t = page * 384L + i * 24L
            val ix =
                events.indexOfFirst {
                    it.track == track && it.bank == bank && it.pad == pad && it.tick == t
                }
            val on = ix >= 0
            val vel = if (on) events[ix].velocity else 127
            b.text = "${i+1}\n${if(on)"● $vel" else "○"}"
            b.setOnClickListener {
                val hit =
                    events.indexOfFirst {
                        it.track == track && it.bank == bank && it.pad == pad && it.tick == t
                    }
                history.checkpoint(events)
                if (hit >= 0) events.removeAt(hit)
                else {
                    events += Event(bank, pad, t, track, 127)
                    report("step_added")
                }
                saveProject()
                redrawSteps(page, label, bs)
            }
        }
    }

    private fun browse() {
        startActivityForResult(
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "audio/*"
            },
            901,
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(r: Int, c: Int, d: Intent?) {
        super.onActivityResult(r, c, d)
        if (r != 901 || c != RESULT_OK) return
        val u = d?.data ?: return
        pending = u
        pendingName = name(u)
        browserPreview.stop()
        val dialog =
            AlertDialog.Builder(this)
                .setTitle("BROWSER • $pendingName")
                .setMessage(
                    "Прослушать или загрузить в BANK ${'A'+bank} PAD ${pad+1}.\n\nPREVIEW и пэды используют один аудиодвижок и общий MASTER. После LOAD: PAD VOL 100%, PITCH 0. Полный WAV/MP3 без обрезки SoundPool."
                )
                .setNegativeButton("ОТМЕНА", null)
                .setNeutralButton("PREVIEW", null)
                .setPositiveButton("LOAD TO PAD", null)
                .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                browserPreview.play(
                    u,
                    onReady = {
                        runOnUiThread { showDisplayMessage("BROWSER • PREVIEW • $pendingName") }
                    },
                    onError = { msg ->
                        runOnUiThread { Toast.makeText(this, msg, Toast.LENGTH_LONG).show() }
                    },
                )
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                browserPreview.stop()
                loadPending()
                dialog.dismiss()
            }
        }
        dialog.setOnDismissListener { browserPreview.stop() }
        dialog.show()
    }

    private fun loadPending() {
        val uri = pending ?: return
        val targetBank = bank
        val targetPad = pad
        val label = pendingName
        if (!importingPads.add(targetBank to targetPad)) {
            showDisplayMessage("Этот пэд ещё загружается. Дождись завершения.")
            return
        }
        showDisplayMessage("BROWSE • загружаю $label…")
        importWorker.execute {
            val file =
                File(
                    File(filesDir, "samples").apply { mkdirs() },
                    "${java.util.UUID.randomUUID()}_${label.replace(Regex("[^A-Za-z0-9._-]"),"_")}",
                )
            try {
                contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { input.copyTo(it) }
                } ?: error("Не удалось открыть аудиофайл")
                runOnUiThread {
                    if (isFinishing || isDestroyed) {
                        file.delete()
                        return@runOnUiThread
                    }
                    val id = pool.load(file.absolutePath, 1)
                    pendingAssignments[id] =
                        PadAssignment(
                            targetBank,
                            targetPad,
                            PadSample(id, label.substringBeforeLast('.'), file.absolutePath),
                        )
                }
            } catch (error: Exception) {
                file.delete()
                runOnUiThread {
                    if (!isDestroyed) {
                        importingPads.remove(targetBank to targetPad)
                        showDisplayMessage("BROWSE • ${error.message}")
                        Toast.makeText(this, error.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    fun currentBpm(): Int = bpm.coerceIn(40, 240)

    fun setTempoBpm(value: Int) {
        val now = SystemClock.elapsedRealtime()
        val next = value.coerceIn(40, 240)
        if (countingIn && next != bpm) {
            countInToken++
            countingIn = false
            showDisplayMessage("TEMPO изменён • нажми PLAY для нового отсчёта")
        }
        if (playing) startMs = now - TrainerPlayback.retimedElapsed(now - startMs, bpm, next)
        bpm = next
        saveProject()
        refreshStatus()
    }

    fun selectedPadVolumePercent(): Int = (padVolumes[bank][pad] * 100f).toInt().coerceIn(0, 100)

    fun setSelectedPadVolumePercent(value: Int) {
        padVolumes[bank][pad] = (value.coerceIn(0, 100) / 100f)
        saveVolumes()
        report("mixer_changed")
        refreshStatus()
    }

    fun selectedPadLabel(): String = "${'A'+bank}${pad+1}"

    private fun sampleEdit() {
        val s = pads[bank][pad]
        if (s == null) {
            Toast.makeText(this, "Сначала загрузите WAV или MP3 на пэд", Toast.LENGTH_SHORT).show()
            return
        }
        val editBank = bank
        val editPad = pad
        val src = File(s.path)
        status.text = "SAMPLE EDIT • preparing ${s.name}…"
        Thread {
                try {
                    val editable =
                        AudioImport.toEditableWav(
                            src,
                            File(filesDir, "sample_edit/${src.nameWithoutExtension}_edit.wav"),
                        )
                    WavSlicer.inspect(editable)
                    runOnUiThread {
                        if (isFinishing || isDestroyed) return@runOnUiThread
                        displayWaveform?.loadPcm16Wav(editable)
                        displayWaveform?.setSelection(0f, 1f)
                        displayWaveform?.visibility = View.VISIBLE
                        displayInfo?.visibility = View.VISIBLE
                        displayInfo?.text =
                            "SAMPLE EDIT   BANK ${'A'+bank} / PAD ${pad+1}   ${s.name}"
                        status.visibility = View.GONE
                        showSampleEditor(editable, s.name, editBank, editPad)
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        showDisplayMessage("SAMPLE EDIT • ${e.message}")
                        Toast.makeText(this, "SAMPLE EDIT: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .start()
    }

    private fun showSampleEditor(src: File, label: String) = showSampleEditor(src, label, bank, pad)

    private fun showSampleEditor(src: File, label: String, targetBank: Int, targetPad: Int) {
        val box =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24, 8, 24, 8)
            }
        val read = TextView(this)
        val waveform =
            WaveformView(this).apply {
                minimumHeight = (150 * resources.displayMetrics.density).toInt()
                loadPcm16Wav(src)
            }
        val start =
            SeekBar(this).apply {
                max = 95
                progress = 0
            }
        val end =
            SeekBar(this).apply {
                max = 100
                progress = 100
            }
        val pieces =
            SeekBar(this).apply {
                max = 14
                progress = 6
            }
        var fromWave = false
        fun update() {
            if (end.progress <= start.progress)
                end.progress = (start.progress + 1).coerceAtMost(100)
            if (!fromWave) {
                waveform.setSelection(start.progress / 100f, end.progress / 100f)
                displayWaveform?.setSelection(start.progress / 100f, end.progress / 100f)
            }
            read.text =
                "START ${start.progress}%   END ${end.progress}%\nCHOPS ${pieces.progress+2} • первый слайс → ${'A'+targetBank}${targetPad+1}"
        }
        waveform.onSelectionChanged = { a, b ->
            fromWave = true
            start.progress = (a * 100).toInt().coerceIn(0, 95)
            end.progress = (b * 100).toInt().coerceIn(start.progress + 1, 100)
            fromWave = false
            update()
        }
        val l =
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) {
                    update()
                }

                override fun onStartTrackingTouch(s: SeekBar?) {}

                override fun onStopTrackingTouch(s: SeekBar?) {}
            }
        start.setOnSeekBarChangeListener(l)
        end.setOnSeekBarChangeListener(l)
        pieces.setOnSeekBarChangeListener(l)
        box.addView(
            TextView(this).apply {
                text =
                    "$label\nДвигай жёлтые границы прямо на волне или используй Start/End. CHOP разложит выбранный участок по пэдам."
            }
        )
        box.addView(
            waveform,
            LinearLayout.LayoutParams(-1, (170 * resources.displayMetrics.density).toInt()),
        )
        box.addView(read)
        box.addView(TextView(this).apply { text = "START" })
        box.addView(start)
        box.addView(TextView(this).apply { text = "END" })
        box.addView(end)
        box.addView(TextView(this).apply { text = "КОЛИЧЕСТВО CHOPS" })
        box.addView(pieces)
        update()
        val dialog =
            AlertDialog.Builder(this)
                .setTitle("SAMPLE EDIT")
                .setView(box)
                .setNegativeButton("CANCEL", null)
                .setNeutralButton("PREVIEW", null)
                .setPositiveButton("CHOP → PADS", null)
                .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                Thread {
                        try {
                            val f =
                                WavSlicer.slice(
                                    src,
                                    File(cacheDir, "sample_edit_preview.wav"),
                                    start.progress / 100f,
                                    end.progress / 100f,
                                )
                            runOnUiThread {
                                browserPreview.play(
                                    Uri.fromFile(f),
                                    onReady = {
                                        showDisplayMessage("SAMPLE EDIT • PREVIEW • $label")
                                    },
                                    onError = { msg ->
                                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                                    },
                                )
                            }
                        } catch (e: Exception) {
                            runOnUiThread {
                                Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                    .start()
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val count = pieces.progress + 2
                if (count > 16 - targetPad) {
                    Toast.makeText(
                            this,
                            "Недостаточно пэдов: нужно $count, доступно ${16-targetPad}",
                            Toast.LENGTH_LONG,
                        )
                        .show()
                    return@setOnClickListener
                }
                val targets = (targetPad until targetPad + count).map { targetBank to it }
                if (targets.any { it in importingPads }) {
                    Toast.makeText(
                            this,
                            "Дождись загрузки звуков на выбранные пэды",
                            Toast.LENGTH_LONG,
                        )
                        .show()
                    return@setOnClickListener
                }
                val from = start.progress / 100f
                val to = end.progress / 100f
                val chop = {
                    importingPads.addAll(targets)
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                    importWorker.execute {
                        val dir = File(filesDir, "samples/chops_${java.util.UUID.randomUUID()}")
                        try {
                            val outputs = WavSlicer.chop(src, dir, from, to, count, "slice")
                            runOnUiThread {
                                if (isFinishing || isDestroyed) {
                                    dir.deleteRecursively()
                                    return@runOnUiThread
                                }
                                outputs.forEachIndexed { i, file ->
                                    val id = pool.load(file.absolutePath, 1)
                                    pendingAssignments[id] =
                                        PadAssignment(
                                            targetBank,
                                            targetPad + i,
                                            PadSample(
                                                id,
                                                file.nameWithoutExtension,
                                                file.absolutePath,
                                            ),
                                            "chopped",
                                        )
                                }
                                showDisplayMessage(
                                    "CHOP • $count слайсов → BANK ${'A'+targetBank}, PAD ${targetPad+1}…${targetPad+count}"
                                )
                                dialog.dismiss()
                            }
                        } catch (error: Exception) {
                            dir.deleteRecursively()
                            runOnUiThread {
                                if (!isDestroyed) {
                                    importingPads.removeAll(targets.toSet())
                                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                                    Toast.makeText(this, error.message, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }
                val replaced = targets.count { pads[it.first][it.second] != null }
                if (replaced > 0)
                    AlertDialog.Builder(this)
                        .setTitle("CHOP → PADS")
                        .setMessage(
                            "Заменить $replaced назначений в BANK ${'A'+targetBank}, PAD ${targetPad+1}…${targetPad+count}? Исходный аудиофайл останется в проекте."
                        )
                        .setNegativeButton("CANCEL", null)
                        .setPositiveButton("CHOP") { _, _ -> chop() }
                        .show()
                else chop()
            }
        }
        dialog.show()
    }

    private fun refreshPads() {
        padButtons.forEachIndexed { i, b -> b.text = "${i+1}\n${pads[bank][i]?.name?:"EMPTY"}" }
        refreshStatus()
    }

    fun setWorkspaceMode(mode: String) {
        workspaceMode = mode
        displayOverride = null
        if (mode != "SAMPLE EDIT") {
            displayWaveform?.visibility = View.GONE
            displayInfo?.visibility = View.GONE
            status.visibility = View.VISIBLE
        }
        refreshStatus()
    }

    fun showDisplayMessage(message: String) {
        displayOverride = message
        refreshStatus()
    }

    fun clearDisplayMessage() {
        displayOverride = null
        refreshStatus()
    }

    private fun position(): String {
        val t = tick()
        val bar = (t / 384L) + 1
        val within = t % 384L
        val beat = (within / 96L) + 1
        val pulse = within % 96L
        return "%03d:%02d:%02d".format(bar, beat, pulse)
    }

    private fun stepPattern(bar: Int): String {
        val start = bar * 384L
        return (0 until 16).joinToString("") { step ->
            val t = start + step * 24L
            if (
                events.any { it.track == track && it.bank == bank && it.pad == pad && it.tick == t }
            )
                "■"
            else "·"
        }
    }

    private fun workspaceText(): String {
        val selected = pads[bank][pad]
        val transport =
            if (recording) if (overdub) "OVERDUB" else "REC" else if (playing) "PLAY" else "STOP"
        return when (workspaceMode) {
            "BROWSE" ->
                "BROWSE  •  BANK ${'A'+bank} / PAD ${pad+1}\n${selected?.name?:"EMPTY"}\nВыбери WAV или MP3 • PREVIEW → LOAD TO PAD"
            "SAMPLE EDIT" ->
                "SAMPLE EDIT  •  BANK ${'A'+bank} / PAD ${pad+1}\n${selected?.name?:"EMPTY"}\nSTART / END • PREVIEW • CHOP → PADS"
            "STEP SEQ" ->
                "STEP SEQ • TRACK ${track+1} • ${'A'+bank}${pad+1}\n1 ${stepPattern(0)}\n2 ${stepPattern(1)}"
            "MIXER" ->
                "MIXER  •  MASTER / PROJECT\nBANK ${'A'+bank} • PAD ${pad+1} • ${selected?.name?:"EMPTY"}\nГромкость проекта и выбранного пэда"
            else ->
                "MAIN  •  SEQ 01  •  TRACK ${track+1}${if(mute[track])" MUTE" else ""}\nBANK ${'A'+bank} • PAD ${pad+1} • ${position()} • $transport\n$bpm BPM • TC 1/$tc • SWING $swing%\nMET ${if(metronome)"ON" else "OFF"} • COUNT ${if(countIn)"ON" else "OFF"}"
        }
    }

    private fun refreshStatus() {
        displayInfo?.apply {
            visibility = View.VISIBLE
            layoutParams = FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM)
            setBackgroundColor(Color.rgb(17, 28, 25))
            text = levelReadout()
            setOnClickListener { showLevelHelp() }
        }
        TrainerUiState.publish(
            bank,
            pad,
            track,
            playing,
            recording,
            overdub,
            noteRepeat,
            fullLevel,
            levels16,
            mute[track],
            recordArmed,
            countingIn,
        )
        status.textSize = if (learn) 14f else 12f
        status.text =
            if (learn)
                "УРОК ${engine.lesson+1}: ${LessonContent.get(engine.lesson).title}\n${displayOverride?:workspaceText()}"
            else displayOverride ?: workspaceText()
    }

    private fun bar(t: Long): String {
        val b = t / 384 + 1
        val beat = (t % 384) / 96 + 1
        val sub = t % 96
        return "%03d:%02d:%02d".format(b, beat, sub)
    }

    private fun name(u: Uri): String {
        contentResolver.query(u, null, null, null, null)?.use { c ->
            val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (c.moveToFirst() && i >= 0) return c.getString(i)
        }
        return "sample.wav"
    }

    private fun savePads() {
        val a = JSONArray()
        for (b in 0..3) for (p in 0..15) pads[b][p]?.let {
            a.put(JSONObject().put("b", b).put("p", p).put("n", it.name).put("path", it.path))
        }
        getSharedPreferences("trainer_v2", MODE_PRIVATE)
            .edit()
            .putString("pads", a.toString())
            .apply()
    }

    private fun restorePads() {
        val raw = getSharedPreferences("trainer_v2", MODE_PRIVATE).getString("pads", null) ?: return
        try {
            val a = JSONArray(raw)
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                val f = File(o.getString("path"))
                val rb = o.optInt("b", -1)
                val rp = o.optInt("p", -1)
                if (f.exists() && rb in 0..3 && rp in 0..15)
                    pads[rb][rp] =
                        PadSample(
                            pool.load(f.absolutePath, 1),
                            o.optString("n", f.nameWithoutExtension),
                            f.absolutePath,
                        )
            }
        } catch (_: Exception) {}
    }

    private fun saveProject() {
        val o =
            JSONObject()
                .put("velocitySchema", 2)
                .put("bpm", bpm)
                .put("tc", tc)
                .put("swing", swing)
                .put("track", track)
                .put("bank", bank)
                .put("pad", pad)
                .put("position", if (playing) tick() else pausedTick)
                .put("tcOn", tcEnabled)
                .put("tcTriplet", tcTriplet)
                .put("tcShift", tcShift)
                .put("metro", metronome)
                .put("count", countIn)
        val m = JSONArray()
        mute.forEach { m.put(it) }
        o.put("mute", m)
        val a = JSONArray()
        events.forEach {
            a.put(
                JSONObject()
                    .put("b", it.bank)
                    .put("p", it.pad)
                    .put("t", it.tick)
                    .put("tr", it.track)
                    .put("v", it.velocity)
                    .put("tune", it.tuneOffset)
            )
        }
        o.put("events", a)
        getSharedPreferences("trainer_v2", MODE_PRIVATE)
            .edit()
            .putString("project", o.toString())
            .apply()
    }

    private fun restoreProject() {
        val raw =
            getSharedPreferences("trainer_v2", MODE_PRIVATE).getString("project", null) ?: return
        try {
            val o = JSONObject(raw)
            bpm = o.optInt("bpm", 90).coerceIn(40, 240)
            tc = o.optInt("tc", 16).let { if (it in listOf(4, 8, 16, 32, 64)) it else 16 }
            swing = o.optInt("swing", 50).coerceIn(50, 75)
            track = o.optInt("track", 0).coerceIn(0, 3)
            bank = o.optInt("bank", 0).coerceIn(0, 3)
            pad = o.optInt("pad", 0).coerceIn(0, 15)
            pausedTick = o.optLong("position", 0).coerceIn(0, loopTicks() - 1)
            tcEnabled = o.optBoolean("tcOn", true)
            tcTriplet = o.optBoolean("tcTriplet", false)
            tcShift = o.optInt("tcShift", 0).coerceIn(-96, 96)
            metronome = o.optBoolean("metro", false)
            countIn = o.optBoolean("count", true)
            val m = o.optJSONArray("mute")
            if (m != null) for (i in 0 until minOf(4, m.length())) mute[i] = m.optBoolean(i, false)
            val a = o.optJSONArray("events")
            if (a != null)
                for (i in 0 until a.length()) {
                    val e = a.getJSONObject(i)
                    val eb = e.optInt("b", -1)
                    val ep = e.optInt("p", -1)
                    val et = e.optLong("t", -1L)
                    val er = e.optInt("tr", -1)
                    val ev =
                        PadVoice.restoredVelocity(e.optInt("v", 127), o.optInt("velocitySchema", 1))
                    if (eb in 0..3 && ep in 0..15 && er in 0..3 && et >= 0L)
                        events +=
                            Event(
                                eb,
                                ep,
                                et % loopTicks(),
                                er,
                                ev,
                                e.optInt("tune", 0).coerceIn(-15, 15),
                            )
                }
        } catch (_: Exception) {}
    }

    fun showVolumeMixer() {
        val box =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(28, 8, 28, 8)
            }
        val oldMaster = masterVolume
        val oldPad = padVolumes[bank][pad]
        val masterLabel = TextView(this)
        val master =
            SeekBar(this).apply {
                max = 100
                progress = (masterVolume * 100).toInt()
            }
        val padLabel = TextView(this)
        val pv =
            SeekBar(this).apply {
                max = 100
                progress = (padVolumes[bank][pad] * 100).toInt()
            }
        fun update() {
            masterLabel.text = "MASTER / PROJECT  ${master.progress}%"
            padLabel.text = "BANK ${'A'+bank} PAD ${pad+1}  ${pv.progress}%"
        }
        val l =
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, x: Int, f: Boolean) {
                    update()
                }

                override fun onStartTrackingTouch(s: SeekBar?) {}

                override fun onStopTrackingTouch(s: SeekBar?) {}
            }
        master.setOnSeekBarChangeListener(l)
        pv.setOnSeekBarChangeListener(l)
        box.addView(masterLabel)
        box.addView(master)
        box.addView(padLabel)
        box.addView(pv)
        update()
        AlertDialog.Builder(this)
            .setTitle("MIXER / VOLUME")
            .setView(box)
            .setNeutralButton("100% / 100%") { _, _ -> restoreNormalLevels() }
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("APPLY") { _, _ ->
                val newMaster = master.progress / 100f
                val newPad = pv.progress / 100f
                masterVolume = newMaster
                padVolumes[bank][pad] = newPad
                saveVolumes()
                if (newMaster != oldMaster || newPad != oldPad) report("mixer_changed")
                refreshStatus()
            }
            .show()
    }

    fun adjustSelectedPadVolume(delta: Int): Int {
        val current = kotlin.math.round(padVolumes[bank][pad] * 100f).toInt()
        val v = (current + delta).coerceIn(0, 100)
        val old = padVolumes[bank][pad]
        padVolumes[bank][pad] = v / 100f
        saveVolumes()
        if (old != padVolumes[bank][pad]) report("mixer_changed")
        return v
    }

    private fun saveVolumes() {
        val o = JSONObject().put("master", masterVolume)
        val a = JSONArray()
        for (b in 0..3) for (p in 0..15) a.put(padVolumes[b][p])
        o.put("pads", a)
        val pitch = JSONArray()
        for (b in 0..3) for (p in 0..15) pitch.put(padPitch[b][p])
        o.put("pitch", pitch)
        getSharedPreferences("trainer_v2", MODE_PRIVATE)
            .edit()
            .putString("volumes", o.toString())
            .apply()
    }

    private fun restoreVolumes() {
        val raw =
            getSharedPreferences("trainer_v2", MODE_PRIVATE).getString("volumes", null) ?: return
        try {
            val o = JSONObject(raw)
            masterVolume = o.optDouble("master", 1.0).toFloat().coerceIn(0f, 1f)
            val a = o.optJSONArray("pads") ?: return
            for (b in 0..3) for (p in 0..15) {
                val i = b * 16 + p
                if (i < a.length())
                    padVolumes[b][p] = a.optDouble(i, 1.0).toFloat().coerceIn(0f, 1f)
            }
            val pitch = o.optJSONArray("pitch")
            if (pitch != null)
                for (b in 0..3) for (p in 0..15) {
                    val i = b * 16 + p
                    if (i < pitch.length()) padPitch[b][p] = pitch.optInt(i, 0).coerceIn(-12, 12)
                }
        } catch (_: Exception) {}
    }

    fun prepareForNewProject() {
        stopTransport()
        resettingProject = true
    }

    override fun onPause() {
        if (!resettingProject && ::engine.isInitialized && ::status.isInitialized) {
            stopTransport()
            pool.stopAll()
            browserPreview.stop()
            savePads()
            saveVolumes()
        }
        super.onPause()
    }

    override fun onDestroy() {
        countInToken++
        h.removeCallbacksAndMessages(null)
        importWorker.shutdownNow()
        pendingAssignments.values.forEach { File(it.sample.path).delete() }
        pendingAssignments.clear()
        importingPads.clear()
        browserPreview.release()
        try {
            pool.setOnLoadCompleteListener(null)
        } catch (_: Exception) {}
        try {
            pool.release()
        } catch (_: Exception) {}
        try {
            tone.release()
        } catch (_: Exception) {}
        readySounds.clear()
        pendingAuditions.clear()
        super.onDestroy()
    }
}

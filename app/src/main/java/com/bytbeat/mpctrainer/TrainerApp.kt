package com.bytbeat.mpctrainer

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog

/** Phone surface: stable workspace, full-size pads, compact hardware-like controls. */
class TrainerApp : Application(), Application.ActivityLifecycleCallbacks {
    private val ui = Handler(Looper.getMainLooper())
    private val ledStates = java.util.WeakHashMap<Button, Triple<Boolean, Int, Boolean>>()
    private var pulse: Runnable? = null
    private var lastPadSerial = -1L
    private var litPad = -1
    private var litSerial = -1L
    private var padKnob: PadVolumeKnob? = null

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityResumed(activity: Activity) {
        if (activity !is TrainerActivity) return
        val content = activity.findViewById<FrameLayout>(android.R.id.content) ?: return
        if (content.tag != "phone_ui_v26") {
            val root = content.getChildAt(0) as? LinearLayout ?: return
            rebuild(activity, root)
            content.tag = "phone_ui_v26"
        }
        syncKnob(activity)
        startIndicators(activity)
    }

    override fun onActivityPaused(a: Activity) {
        stopIndicators()
    }

    private fun rebuild(a: TrainerActivity, root: LinearLayout) {
        root.setPadding(a.dp(8), 0, a.dp(8), 0)
        root.setBackgroundColor(Color.rgb(202, 198, 187))
        findText(root) { it.text?.toString()?.startsWith("MPC TRAINER") == true }
            ?.apply {
                text = "MPC УЧЁБА · ${BuildConfig.VERSION_CODE}"
                textSize = 13f
                typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
                letterSpacing = -0.035f
                gravity = Gravity.CENTER_VERTICAL
                setTextColor(Color.rgb(190, 35, 32))
                layoutParams = layoutParams.apply { height = a.dp(22) }
            }
        val displayFrame =
            (0 until root.childCount)
                .mapNotNull { root.getChildAt(it) as? FrameLayout }
                .firstOrNull()
        val displayText = displayFrame?.let { findFirst(it, TextView::class.java) }
        val displayH = a.dp(if (a.resources.configuration.screenHeightDp < 740) 84 else 100)
        displayText?.apply {
            textSize = 10.5f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.TOP or Gravity.START
            minHeight = 0
            setPadding(a.dp(12), a.dp(9), a.dp(12), a.dp(20))
            layoutParams = layoutParams.apply { height = -1 }
            background = rounded(Color.rgb(17, 28, 25), Color.rgb(6, 10, 9), a.dp(2), a.dp(4))
            setTextColor(Color.rgb(190, 235, 179))
        }
        displayFrame?.layoutParams = displayFrame?.layoutParams?.apply { height = displayH }
        val rows =
            (0 until root.childCount)
                .mapNotNull { root.getChildAt(it) as? LinearLayout }
                .filter { it.orientation == LinearLayout.HORIZONTAL }
        rows.forEach { row ->
            val labels =
                (0 until row.childCount).mapNotNull {
                    (row.getChildAt(it) as? Button)?.text?.toString()
                }
            val transport = labels.any { it in setOf("REC", "OVERDUB", "STOP", "PLAY") }
            val bankRow = labels.any { it.startsWith("BANK ") }
            val topModes = labels.any { it in setOf("MAIN", "BROWSE", "SAMPLE EDIT", "LEARN") }
            row.setPadding(0, 0, 0, 0)
            row.layoutParams =
                row.layoutParams.apply {
                    height =
                        if (transport) a.dp(36)
                        else if (bankRow) a.dp(32) else if (topModes) a.dp(32) else a.dp(30)
                }
            for (i in 0 until row.childCount) (row.getChildAt(i) as? Button)?.apply {
                minHeight = 0
                minimumHeight = 0
                textSize = if (transport) 8.8f else 7.6f
                typeface = if (transport) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                isAllCaps = false
                setPadding(a.dp(1), 0, a.dp(1), 0)
                background = if (transport) transportSelector(a) else buttonSelector(a)
                setTextColor(Color.rgb(42, 41, 38))
            }
        }
        val grid = findFirst(root, GridLayout::class.java)
        if (grid != null) {
            val gridSize =
                (a.resources.displayMetrics.widthPixels - a.dp(16)).coerceAtMost(a.dp(460))
            (grid.layoutParams as? LinearLayout.LayoutParams)?.let { lp ->
                lp.height = 0
                lp.weight = 1f
                lp.topMargin = a.dp(2)
                lp.bottomMargin = a.dp(2)
                grid.layoutParams = lp
            }
            val gap = a.dp(4)
            val cell = (gridSize - gap * 3) / 4
            for (i in 0 until grid.childCount) (grid.getChildAt(i) as? Button)?.apply {
                minHeight = 0
                minimumHeight = 0
                textSize = 8.6f
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
                setPadding(a.dp(3), a.dp(3), a.dp(3), a.dp(3))
                setTextColor(Color.rgb(231, 230, 223))
                gravity = Gravity.CENTER
                background = padSelector(a)
                layoutParams =
                    GridLayout.LayoutParams().apply {
                        width = cell
                        height = cell
                        columnSpec = GridLayout.spec(i % 4)
                        rowSpec = GridLayout.spec(3 - i / 4)
                        setMargins(if (i % 4 == 0) 0 else gap, if (i / 4 == 3) 0 else gap, 0, 0)
                    }
            }
        }
        grid?.addOnLayoutChangeListener {
            view,
            left,
            top,
            right,
            bottom,
            oldLeft,
            oldTop,
            oldRight,
            oldBottom ->
            if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) {
                val gap = a.dp(4)
                val side = (minOf(right - left, bottom - top) - gap * 3).coerceAtLeast(0) / 4
                val width = side * 4 + gap * 3
                val size = grid.layoutParams as LinearLayout.LayoutParams
                if (size.width != width) {
                    size.width = width
                    size.gravity = Gravity.CENTER_HORIZONTAL
                    grid.layoutParams = size
                }

                for (i in 0 until grid.childCount) {
                    val lp = grid.getChildAt(i).layoutParams as GridLayout.LayoutParams
                    if (lp.width != side || lp.height != side) {
                        lp.width = side
                        lp.height = side
                        grid.getChildAt(i).layoutParams = lp
                    }
                }
            }
        }

        val restart = findButton(root, "RESTART COURSE")
        val guide = findButton(root, "CONTROL GUIDE")
        restart?.visibility = View.GONE
        guide?.visibility = View.GONE
        rows
            .firstOrNull { row ->
                (0 until row.childCount).any {
                    row.getChildAt(it) === restart || row.getChildAt(it) === guide
                }
            }
            ?.visibility = View.GONE
        addUtilityBar(a, root, restart, guide)
        androidx.core.view.ViewCompat.requestApplyInsets(root)
    }

    private fun addUtilityBar(
        a: TrainerActivity,
        root: LinearLayout,
        restart: Button?,
        guide: Button?,
    ) {
        val panel =
            LinearLayout(a).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
        fun utility(label: String, action: () -> Unit) =
            Button(a).apply {
                text = label
                textSize = 9f
                typeface = Typeface.DEFAULT_BOLD
                isAllCaps = false
                minHeight = 0
                minimumHeight = 0
                setPadding(0, 0, 0, 0)
                background = darkUtilitySelector(a)
                setTextColor(Color.rgb(42, 41, 38))
                setOnClickListener { action() }
            }
        val left = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
        left.addView(
            utility("MENU") { showMenu(a, restart, guide) },
            LinearLayout.LayoutParams(-1, a.dp(35)).apply { bottomMargin = a.dp(3) },
        )
        left.addView(utility("TEMPO") { showBpm(a) }, LinearLayout.LayoutParams(-1, a.dp(35)))
        val right = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
        fun pairRow(leftButton: Button, rightButton: Button) =
            LinearLayout(a).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(
                    leftButton,
                    LinearLayout.LayoutParams(0, a.dp(35), 1f).apply { rightMargin = a.dp(3) },
                )
                addView(rightButton, LinearLayout.LayoutParams(0, a.dp(35), 1f))
            }
        right.addView(
            pairRow(
                utility("PITCH −") { a.adjustSelectedPadPitch(-1) },
                utility("VOL −") {
                    val v = a.adjustSelectedPadVolume(-5)
                    a.showPadVolumeOnDisplay(v)
                },
            ),
            LinearLayout.LayoutParams(-1, a.dp(35)).apply { bottomMargin = a.dp(3) },
        )
        right.addView(
            pairRow(
                utility("PITCH +") { a.adjustSelectedPadPitch(1) },
                utility("VOL +") {
                    val v = a.adjustSelectedPadVolume(5)
                    a.showPadVolumeOnDisplay(v)
                },
            ),
            LinearLayout.LayoutParams(-1, a.dp(35)),
        )
        panel.addView(
            left,
            LinearLayout.LayoutParams(0, a.dp(73), 1f).apply { rightMargin = a.dp(4) },
        )
        panel.addView(right, LinearLayout.LayoutParams(0, a.dp(73), 1f))
        root.addView(panel, LinearLayout.LayoutParams(-1, a.dp(73)).apply { topMargin = a.dp(5) })
    }

    private fun syncKnob(a: TrainerActivity) {
        padKnob?.apply {
            label = "${a.selectedPadLabel()} VOL"
            setValueSilently(a.selectedPadVolumePercent())
        }
    }

    private fun showMenu(a: TrainerActivity, restart: Button?, guide: Button?) {
        val items =
            arrayOf(
                "PITCH RESET",
                "TC ON / OFF",
                "TC • DO IT (CURRENT TRACK)",
                "MIXER / MASTER VOLUME",
                "CONTROL GUIDE",
                "RESTART COURSE",
                "NEW PROJECT",
                "УЧЕБНЫЕ ЗВУКИ • kick / snare / hat / sample",
                "PLAY START • с начала",
                "UNDO • отменить изменение Sequence",
                "REDO • вернуть изменение Sequence",
            )
        AlertDialog.Builder(a)
            .setTitle("MPC TRAINER • MENU")
            .setItems(items) { _, w ->
                when (w) {
                    0 -> a.resetSelectedPadPitch()
                    1 -> a.setTimingCorrectEnabled(!a.timingCorrectEnabled())
                    2 -> a.quantizeCurrentTrack()
                    3 -> {
                        a.setWorkspaceMode("MIXER")
                        a.showVolumeMixer()
                    }
                    4 -> guide?.performClick()
                    5 -> restart?.performClick()
                    6 -> confirmNewProject(a)
                    7 -> a.loadPracticeKit()
                    8 -> a.playFromStart()
                    9 -> a.undoSequence()
                    10 -> a.undoSequence(true)
                }
            }
            .setNegativeButton("ЗАКРЫТЬ", null)
            .show()
    }

    private fun startIndicators(a: TrainerActivity) {
        stopIndicators()
        val root = a.findViewById<ViewGroup>(android.R.id.content) ?: return
        val grid = findFirst(root, GridLayout::class.java)
        val r =
            object : Runnable {
                override fun run() {
                    LearnSurfaceStyler.update(a, root)
                    val s = TrainerUiState.snapshot
                    syncKnob(a)
                    for (i in 0..3) setLed(
                        a,
                        root,
                        "BANK ${'A'+i}",
                        s.bank == i,
                        Color.rgb(35, 167, 210),
                    )
                    setLed(
                        a,
                        root,
                        "REC",
                        (s.recording || s.recordArmed) && !s.overdub,
                        Color.rgb(226, 55, 45),
                        true,
                    )
                    setLed(
                        a,
                        root,
                        "OVERDUB",
                        (s.recording || s.recordArmed) && s.overdub,
                        Color.rgb(237, 125, 31),
                        true,
                    )
                    setLed(a, root, "PLAY", s.playing && !s.recording, Color.rgb(64, 188, 91), true)
                    setLed(a, root, "NOTE REPEAT", s.noteRepeat, Color.rgb(216, 62, 139))
                    setLed(a, root, "FULL LEVEL", s.fullLevel, Color.rgb(229, 175, 48))
                    setLed(a, root, "16 LEVELS", s.levels16, Color.rgb(43, 183, 207))
                    setLed(a, root, "TRACK MUTE", s.trackMuted, Color.rgb(218, 59, 52))
                    if (s.activeSerial != lastPadSerial) {
                        lastPadSerial = s.activeSerial
                        if (s.activeBank == s.bank && s.activePad in 0..15) {
                            val pad = s.activePad
                            val serial = s.activeSerial
                            if (litPad >= 0 && litPad != pad) paintPad(a, grid, litPad, false)
                            litPad = pad
                            litSerial = serial
                            paintPad(a, grid, pad, true)
                            ui.postDelayed(
                                {
                                    if (litPad == pad && litSerial == serial) {
                                        paintPad(a, grid, pad, false)
                                        litPad = -1
                                        litSerial = -1
                                    }
                                },
                                90,
                            )
                        }
                    }
                    ui.postDelayed(this, 50)
                }
            }
        pulse = r
        ui.post(r)
    }

    private fun paintPad(a: Activity, g: GridLayout?, i: Int, on: Boolean) {
        (g?.getChildAt(i) as? Button)?.background =
            if (on) rounded(Color.rgb(69, 74, 67), Color.rgb(37, 201, 217), a.dp(3), a.dp(5))
            else padSelector(a)
    }

    private fun setLed(a: Activity, r: View, l: String, on: Boolean, c: Int, t: Boolean = false) {
        findButton(r, l)?.apply {
            val state = Triple(on, c, t)
            if (ledStates[this] == state) return
            ledStates[this] = state
            background =
                if (on) ledButton(a, c, t) else if (t) transportSelector(a) else buttonSelector(a)
            setTextColor(
                if (on) Color.rgb(18, 18, 17)
                else if (l == "REC") Color.rgb(190, 35, 32) else Color.rgb(42, 41, 38)
            )
        }
    }

    private fun stopIndicators() {
        pulse?.let { ui.removeCallbacks(it) }
        pulse = null
        litPad = -1
        litSerial = -1
    }

    private fun padSelector(a: Activity) =
        StateListDrawable().apply {
            addState(
                intArrayOf(android.R.attr.state_pressed),
                rounded(Color.rgb(67, 68, 63), Color.rgb(37, 201, 217), a.dp(3), a.dp(5)),
            )
            addState(
                intArrayOf(),
                rounded(Color.rgb(55, 54, 51), Color.rgb(91, 88, 81), a.dp(1), a.dp(5)),
            )
        }

    private fun buttonSelector(a: Activity) =
        StateListDrawable().apply {
            addState(
                intArrayOf(android.R.attr.state_pressed),
                rounded(Color.rgb(172, 171, 164), Color.rgb(95, 92, 86), a.dp(1), a.dp(3)),
            )
            addState(
                intArrayOf(),
                rounded(Color.rgb(190, 189, 182), Color.rgb(137, 134, 126), a.dp(1), a.dp(3)),
            )
        }

    private fun transportSelector(a: Activity) = buttonSelector(a)

    private fun darkUtilitySelector(a: Activity) = buttonSelector(a)

    private fun ledButton(a: Activity, c: Int, t: Boolean = false) =
        rounded(Color.rgb(181, 188, 184), c, a.dp(3), a.dp(3))

    private fun showBpm(a: TrainerActivity) {
        val cur = a.currentBpm()
        val title =
            TextView(a).apply {
                text = "TEMPO   $cur BPM"
                textSize = 22f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setPadding(a.dp(12), a.dp(14), a.dp(12), a.dp(8))
                setTextColor(Color.rgb(42, 41, 38))
            }
        val seek =
            SeekBar(a).apply {
                max = 200
                progress = cur - 40
                setPadding(a.dp(18), 0, a.dp(18), 0)
            }
        val box =
            LinearLayout(a).apply {
                orientation = LinearLayout.VERTICAL
                addView(title, LinearLayout.LayoutParams(-1, a.dp(58)))
                addView(seek, LinearLayout.LayoutParams(-1, a.dp(54)))
            }
        seek.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(v: SeekBar?, progress: Int, from: Boolean) {
                    val x = progress + 40
                    title.text = "TEMPO   $x BPM"
                    if (from) a.setTempoBpm(x)
                }

                override fun onStartTrackingTouch(v: SeekBar?) {}

                override fun onStopTrackingTouch(v: SeekBar?) {}
            }
        )
        AlertDialog.Builder(a)
            .setView(box)
            .setNegativeButton("ОТМЕНА") { _, _ -> a.setTempoBpm(cur) }
            .setPositiveButton("ГОТОВО") { _, _ -> a.setTempoBpm(seek.progress + 40) }
            .setOnCancelListener { a.setTempoBpm(cur) }
            .show()
    }

    private fun confirmNewProject(a: TrainerActivity) {
        AlertDialog.Builder(a)
            .setTitle("NEW PROJECT")
            .setMessage("Очистить пэды, Sequence, громкости и настройки проекта?")
            .setNegativeButton("ОТМЕНА", null)
            .setPositiveButton("NEW PROJECT") { _, _ ->
                a.prepareForNewProject()
                val e = a.getSharedPreferences("trainer_v2", Context.MODE_PRIVATE).edit()
                ProjectReset.preferenceKeys().forEach(e::remove)
                e.apply()
                a.getSharedPreferences("learn", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("new_project_pending", true)
                    .apply()
                a.recreate()
            }
            .show()
    }

    private fun rounded(f: Int, s: Int, w: Int, r: Int) =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(f)
            setStroke(w, s)
            cornerRadius = r.toFloat()
        }

    private fun findButton(root: View, label: String) =
        findFirst(root, Button::class.java) { it.text?.toString() == label }

    private fun findText(root: View, p: (TextView) -> Boolean) =
        findFirst(root, TextView::class.java, p)

    private fun <T : View> findFirst(root: View, type: Class<T>, p: (T) -> Boolean = { true }): T? {
        if (type.isInstance(root)) {
            val v = type.cast(root)
            if (v != null && p(v)) return v
        }
        if (root is ViewGroup)
            for (i in 0 until root.childCount) {
                val f = findFirst(root.getChildAt(i), type, p)
                if (f != null) return f
            }
        return null
    }

    private fun Activity.dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()

    override fun onActivityCreated(a: Activity, b: Bundle?) {}

    override fun onActivityStarted(a: Activity) {}

    override fun onActivityStopped(a: Activity) {}

    override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {}

    override fun onActivityDestroyed(a: Activity) {
        stopIndicators()
    }
}

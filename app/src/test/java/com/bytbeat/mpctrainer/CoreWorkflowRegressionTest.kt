package com.bytbeat.mpctrainer

import android.app.Application
import android.net.Uri
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import java.io.File
import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class CoreWorkflowRegressionTest {
    private fun get(a: TrainerActivity, n: String) =
        TrainerActivity::class.java.getDeclaredField(n).apply { isAccessible = true }.get(a)

    private fun set(a: TrainerActivity, n: String, v: Any) {
        TrainerActivity::class.java.getDeclaredField(n).apply { isAccessible = true }.set(a, v)
    }

    private fun invoke(a: TrainerActivity, n: String): Any? =
        TrainerActivity::class.java.getDeclaredMethod(n).apply { isAccessible = true }.invoke(a)

    @Suppress("UNCHECKED_CAST")
    private fun notes(a: TrainerActivity) = get(a, "events") as MutableList<TrainerActivity.Event>

    @Suppress("UNCHECKED_CAST")
    private fun pads(a: TrainerActivity) =
        get(a, "pads") as Array<Array<TrainerActivity.PadSample?>>

    private fun open(): org.robolectric.android.controller.ActivityController<TrainerActivity> {
        val app = RuntimeEnvironment.getApplication()
        for (n in listOf("mpc", "trainer_v2", "learn")) app.getSharedPreferences(n, 0)
            .edit()
            .clear()
            .commit()
        app.getSharedPreferences("mpc", 0).edit().putInt("course_welcome", 1000).commit()
        return Robolectric.buildActivity(TrainerActivity::class.java).setup()
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun settleImports(a: TrainerActivity) {
        idle() // Dialog buttons enqueue work on the main looper before the I/O job exists.
        (get(a, "importWorker") as ExecutorService).submit {}.get(10, TimeUnit.SECONDS)
        idle()
        val engine = get(a, "pool") as UnifiedAudioEngine
        val worker =
            UnifiedAudioEngine::class
                .java
                .getDeclaredField("worker")
                .apply { isAccessible = true }
                .get(engine) as ExecutorService
        worker.submit {}.get(10, TimeUnit.SECONDS)
        idle()
    }

    private fun all(v: View): List<View> =
        listOf(v) +
            if (v is ViewGroup) (0 until v.childCount).flatMap { all(v.getChildAt(it)) }
            else emptyList()

    private fun button(a: TrainerActivity, label: String) =
        all(a.window.decorView).filterIsInstance<Button>().first { it.text.toString() == label }

    private fun touch(a: TrainerActivity, pad: Int, action: Int) {
        val buttons = get(a, "padButtons") as List<*>
        val now = SystemClock.uptimeMillis()
        val e = MotionEvent.obtain(now, now, action, 12f, 12f, 0)
        try {
            (buttons[pad] as Button).dispatchTouchEvent(e)
        } finally {
            e.recycle()
        }
    }

    @Test
    fun resumeDoesNotFireEveryNoteBeforeThePausedPosition() {
        val c = open()
        val a = c.get()
        try {
            notes(a).add(TrainerActivity.Event(0, 0, 0, 0, 127))
            notes(a).add(TrainerActivity.Event(0, 1, 24, 0, 127))
            set(a, "pausedTick", 200L)
            val serial = TrainerUiState.snapshot.activeSerial
            invoke(a, "play")
            idle()
            assertEquals(serial, TrainerUiState.snapshot.activeSerial)
            assertTrue((invoke(a, "tick") as Long) >= 200)
            invoke(a, "stopTransport")
            assertTrue((invoke(a, "tick") as Long) >= 200)
            a.playFromStart()
            idle()
            assertTrue(TrainerUiState.snapshot.activeSerial > serial)
            assertTrue((invoke(a, "tick") as Long) < 10)
        } finally {
            c.pause().stop().destroy()
        }
    }

    @Test
    fun stoppedPositionAndSelectedPadSurviveRelaunch() {
        var c = open()
        var a = c.get()
        try {
            set(a, "pausedTick", 321L)
            set(a, "pad", 9)
            set(a, "bank", 2)
            c.pause().stop().destroy()
            c = Robolectric.buildActivity(TrainerActivity::class.java).setup()
            a = c.get()
            assertEquals(321L, invoke(a, "tick"))
            assertEquals(9, get(a, "pad"))
            assertEquals(2, get(a, "bank"))
        } finally {
            c.pause().stop().destroy()
        }
    }

    private fun invoke(a: TrainerActivity, n: String, arg: Boolean): Any? =
        TrainerActivity::class
            .java
            .getDeclaredMethod(n, Boolean::class.javaPrimitiveType)
            .apply { isAccessible = true }
            .invoke(a, arg)

    @Test
    fun countInCanBeCancelledWithoutErasingTheTrack() {
        val c = open()
        val a = c.get()
        try {
            val original = TrainerActivity.Event(0, 0, 24, 0, 127)
            notes(a).add(original)
            invoke(a, "record", false)
            invoke(a, "play")
            idle()
            assertTrue(get(a, "countingIn") as Boolean)
            val token = get(a, "countInToken")
            invoke(a, "play")
            assertEquals(token, get(a, "countInToken"))
            invoke(a, "stopTransport")
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(4))
            assertFalse(get(a, "playing") as Boolean)
            assertFalse(get(a, "recording") as Boolean)
            assertEquals(listOf(original), notes(a))
        } finally {
            c.pause().stop().destroy()
        }
    }

    @Test
    fun releasingOnePadDoesNotStopTheOtherHeldPadsRepeat() {
        val c = open()
        val a = c.get()
        try {
            a.loadPracticeKit()
            set(a, "countIn", false)
            invoke(a, "record", false)
            invoke(a, "play")
            set(a, "noteRepeat", true)
            touch(a, 0, MotionEvent.ACTION_DOWN)
            touch(a, 1, MotionEvent.ACTION_DOWN)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))
            assertTrue(notes(a).count { it.pad == 0 } >= 2)
            assertTrue(notes(a).count { it.pad == 1 } >= 2)
            touch(a, 1, MotionEvent.ACTION_UP)
            val one = notes(a).count { it.pad == 1 }
            val zero = notes(a).count { it.pad == 0 }
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))
            assertEquals(one, notes(a).count { it.pad == 1 })
            assertTrue(notes(a).count { it.pad == 0 } > zero)
            touch(a, 0, MotionEvent.ACTION_UP)
            val size = notes(a).size
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(400))
            assertEquals(size, notes(a).size)
        } finally {
            c.pause().stop().destroy()
        }
    }

    @Test
    fun undoRestoresTheTrackReplacedByRecButKeepsTheOtherTrack() {
        val c = open()
        val a = c.get()
        try {
            val old = TrainerActivity.Event(0, 0, 24, 0, 127)
            val other = TrainerActivity.Event(0, 1, 48, 1, 90)
            notes(a).addAll(listOf(old, other))
            set(a, "countIn", false)
            invoke(a, "record", false)
            invoke(a, "play")
            idle()
            assertEquals(listOf(other), notes(a))
            assertFalse(a.undoSequence())
            invoke(a, "stopTransport")
            assertTrue(a.undoSequence())
            assertEquals(listOf(old, other), notes(a))
            assertTrue(a.undoSequence(true))
            assertEquals(listOf(other), notes(a))
        } finally {
            c.pause().stop().destroy()
        }
    }

    @Test
    fun leavingTheAppStopsRecordingAndPadRepeat() {
        val c = open()
        val a = c.get()
        try {
            a.loadPracticeKit()
            set(a, "countIn", false)
            invoke(a, "record", false)
            invoke(a, "play")
            set(a, "noteRepeat", true)
            touch(a, 0, MotionEvent.ACTION_DOWN)
            c.pause()
            val size = notes(a).size
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
            assertFalse(get(a, "playing") as Boolean)
            assertFalse(get(a, "recording") as Boolean)
            assertEquals(size, notes(a).size)
        } finally {
            c.stop().destroy()
        }
    }

    @Test
    fun failedImportKeepsThePreviousSoundAndSuccessfulImportKeepsItsTarget() {
        val c = open()
        val a = c.get()
        try {
            a.loadPracticeKit()
            val original = pads(a)[0][0]!!
            val bad = File(a.cacheDir, "broken.wav").apply { writeText("not audio") }
            set(a, "pending", Uri.fromFile(bad))
            set(a, "pendingName", "broken.wav")
            invoke(a, "loadPending")
            settleImports(a)
            assertEquals(original, pads(a)[0][0])
            assertTrue((get(a, "importingPads") as Set<*>).isEmpty())
            set(a, "pending", Uri.fromFile(File(original.path)))
            set(a, "pendingName", "copy.wav")
            invoke(a, "loadPending")
            set(a, "bank", 2)
            settleImports(a)
            assertEquals("copy", pads(a)[0][0]?.name)
            assertNull(pads(a)[2][0])
            assertTrue(File(original.path).exists())
        } finally {
            c.pause().stop().destroy()
        }
    }

    @Test
    fun chopConfirmsReplacementThenProducesPlayableFilesWithoutDeletingTheSource() {
        val c = open()
        val a = c.get()
        try {
            a.loadPracticeKit()
            val source = File(pads(a)[0][0]!!.path)
            TrainerActivity::class
                .java
                .getDeclaredMethod("showSampleEditor", File::class.java, String::class.java)
                .apply { isAccessible = true }
                .invoke(a, source, "kick")
            idle()
            val editor = ShadowDialog.getLatestDialog() as AlertDialog
            all(editor.window!!.decorView).filterIsInstance<SeekBar>().last().progress = 0
            editor.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            val confirm = ShadowDialog.getLatestDialog() as AlertDialog
            assertNotSame(editor, confirm)
            confirm.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            settleImports(a)
            assertTrue(source.exists())
            assertTrue(pads(a)[0][0]!!.path.contains("chops_"))
            assertTrue(pads(a)[0][1]!!.path.contains("chops_"))
            assertTrue(WavSlicer.inspect(File(pads(a)[0][0]!!.path)).frames > 0)
            assertTrue(WavSlicer.inspect(File(pads(a)[0][1]!!.path)).frames > 0)
        } finally {
            c.pause().stop().destroy()
        }
    }

    @Test
    fun lifecycleSaveDoesNotResurrectANewlyClearedProject() {
        val c = open()
        val a = c.get()
        try {
            a.loadPracticeKit()
            notes(a).add(TrainerActivity.Event(0, 0, 24, 0, 127))
            a.prepareForNewProject()
            val prefs = a.getSharedPreferences("trainer_v2", 0)
            ProjectReset.preferenceKeys().forEach { prefs.edit().remove(it).commit() }
            c.pause()
            assertFalse(prefs.contains("pads"))
            assertFalse(prefs.contains("project"))
            assertFalse(prefs.contains("volumes"))
        } finally {
            c.stop().destroy()
        }
    }

    @Test
    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    @Config(application = TrainerApp::class, qualifiers = "w360dp-h640dp")
    fun smallPhoneKeepsPadsTransportAndMenuInsideTheWindow() {
        val c = open()
        val a = c.get()
        try {
            val decor = a.window.decorView
            val d = a.resources.displayMetrics.density
            val w = (360 * d).toInt()
            val h = (640 * d).toInt()
            repeat(3) {
                decor.measure(
                    View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY),
                )
                decor.layout(0, 0, w, h)
                idle()
            }
            val grid = all(decor).filterIsInstance<GridLayout>().first()
            assertTrue(grid.height > 0)
            val p = IntArray(2)
            button(a, "PLAY").getLocationInWindow(p)
            assertTrue("PLAY outside phone: ${p[1]}", p[1] + button(a, "PLAY").height <= h)
            button(a, "MENU").getLocationInWindow(p)
            assertTrue("MENU outside phone", p[1] + button(a, "MENU").height <= h)
            assertEquals(16, grid.childCount)
            assertTrue(
                "MPC pad 1 belongs below pad 13",
                grid.getChildAt(0).top > grid.getChildAt(12).top,
            )
            assertEquals(grid.getChildAt(0).left, grid.getChildAt(12).left)
            assertTrue(grid.getChildAt(0).height >= 28 * d)
            val bitmap =
                android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
            decor.draw(android.graphics.Canvas(bitmap))
            val image =
                File(
                        "build/reports/phone-ui/mpc-360x640-api${android.os.Build.VERSION.SDK_INT}.png"
                    )
                    .apply { parentFile.mkdirs() }
            image.outputStream().use {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
            bitmap.recycle()
            button(a, "PITCH +").performClick()
            assertEquals(1, a.adjustSelectedPadPitch(0))
        } finally {
            c.pause().stop().destroy()
        }
    }

    @Test
    fun trackMuteSuppressesSequencedHitsButNotLiveAudition() {
        val c = open()
        val a = c.get()
        try {
            notes(a).add(TrainerActivity.Event(0, 0, 0, 0, 127))
            (get(a, "mute") as BooleanArray)[0] = true
            val serial = TrainerUiState.snapshot.activeSerial
            invoke(a, "play")
            idle()
            assertEquals(serial, TrainerUiState.snapshot.activeSerial)
            touch(a, 0, MotionEvent.ACTION_DOWN)
            touch(a, 0, MotionEvent.ACTION_UP)
            assertTrue(TrainerUiState.snapshot.activeSerial > serial)
            invoke(a, "stopTransport")
            (get(a, "mute") as BooleanArray)[0] = false
            val live = TrainerUiState.snapshot.activeSerial
            a.playFromStart()
            idle()
            assertTrue(TrainerUiState.snapshot.activeSerial > live)
        } finally {
            c.pause().stop().destroy()
        }
    }

    @Test
    fun tempoChangeDuringCountInDoesNotLeaveARecordingRunningLater() {
        val c = open()
        val a = c.get()
        try {
            invoke(a, "record", false)
            invoke(a, "play")
            idle()
            a.setTempoBpm(120)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(4))
            assertFalse(get(a, "playing") as Boolean)
            assertFalse(get(a, "countingIn") as Boolean)
            assertTrue(get(a, "recordArmed") as Boolean)
        } finally {
            c.pause().stop().destroy()
        }
    }

    @Test
    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    @Config(sdk = [35], application = TrainerApp::class, qualifiers = "w393dp-h850dp")
    fun android15KeepsTheTransportAndPadsVisible() {
        smallPhoneKeepsPadsTransportAndMenuInsideTheWindow()
    }
}

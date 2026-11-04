package com.bytbeat.mpctrainer

import android.app.Application
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Exercise actual state and musical results, not whitespace in generated source. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class PitchTimingCorrectIntegrationTest {
    private fun field(a: TrainerActivity, n: String) =
        TrainerActivity::class.java.getDeclaredField(n).apply { isAccessible = true }

    private fun set(a: TrainerActivity, n: String, v: Any) {
        field(a, n).set(a, v)
    }

    private fun invoke(a: TrainerActivity, n: String): Any? =
        TrainerActivity::class.java.getDeclaredMethod(n).apply { isAccessible = true }.invoke(a)

    private fun open(): org.robolectric.android.controller.ActivityController<TrainerActivity> {
        val app = RuntimeEnvironment.getApplication()
        for (n in listOf("mpc", "trainer_v2", "learn")) app.getSharedPreferences(n, 0)
            .edit()
            .clear()
            .commit()
        app.getSharedPreferences("mpc", 0).edit().putInt("course_welcome", 1000).commit()
        return Robolectric.buildActivity(TrainerActivity::class.java).setup()
    }

    @Test
    fun pitchChangesTheAudibleRateAndPersists() {
        var c = open()
        var a = c.get()
        try {
            assertEquals(-12, a.adjustSelectedPadPitch(-12))
            val event = TrainerActivity.Event(0, 0, 0, 0, 127)
            val voice =
                TrainerActivity::class
                    .java
                    .getDeclaredMethod("voiceParameters", TrainerActivity.Event::class.java)
                    .apply { isAccessible = true }
            assertEquals(.5f, (voice.invoke(a, event) as PadVoice.Parameters).rate, .0001f)
            c.pause().stop().destroy()
            c = Robolectric.buildActivity(TrainerActivity::class.java).setup()
            a = c.get()
            assertEquals(.5f, (voice.invoke(a, event) as PadVoice.Parameters).rate, .0001f)
            a.resetSelectedPadPitch()
            assertEquals(1f, (voice.invoke(a, event) as PadVoice.Parameters).rate, .0001f)
        } finally {
            c.pause().stop().destroy()
        }
    }

    @Test
    fun tripletShiftDoItAffectsOnlySelectedTrackAndCanBeUndone() {
        val c = open()
        val a = c.get()
        try {
            @Suppress("UNCHECKED_CAST")
            val events = field(a, "events").get(a) as MutableList<TrainerActivity.Event>
            events.add(TrainerActivity.Event(0, 0, 10, 0, 127))
            events.add(TrainerActivity.Event(0, 0, 10, 1, 127))
            set(a, "tc", 16)
            set(a, "tcTriplet", true)
            set(a, "tcShift", 5)
            assertEquals(1, a.quantizeCurrentTrack())
            assertEquals(21L, events[0].tick)
            assertEquals(10L, events[1].tick)
            assertTrue(a.undoSequence())
            assertEquals(listOf(10L, 10L), events.map { it.tick })
            assertTrue(a.undoSequence(true))
            assertEquals(listOf(21L, 10L), events.map { it.tick })
            a.setTimingCorrectEnabled(false)
            assertFalse(a.timingCorrectEnabled())
        } finally {
            c.pause().stop().destroy()
        }
    }

    @Test
    fun repeatUsesTheSelectedGridIncludingFastTriplets() {
        val c = open()
        val a = c.get()
        try {
            a.setTempoBpm(90)
            set(a, "tc", 16)
            assertEquals(166L, invoke(a, "noteRepeatIntervalMs"))
            set(a, "tcTriplet", true)
            assertEquals(111L, invoke(a, "noteRepeatIntervalMs"))
            a.setTempoBpm(240)
            set(a, "tc", 64)
            assertEquals(10L, invoke(a, "noteRepeatIntervalMs"))
        } finally {
            c.pause().stop().destroy()
        }
    }

    @Test
    fun fullLevelAndSixteenLevelsExposeTheirOwnVelocityAndTuneControls() {
        val c = open()
        val a = c.get()
        try {
            set(a, "levels16", true)
            set(a, "levels16Type", "VELOCITY")
            val velocity =
                TrainerActivity::class
                    .java
                    .getDeclaredMethod("velocity", Int::class.javaPrimitiveType)
                    .apply { isAccessible = true }
            assertEquals(8, velocity.invoke(a, 0))
            assertEquals(127, velocity.invoke(a, 15))
            set(a, "levels16", false)
            set(a, "fullLevel", true)
            assertEquals(127, velocity.invoke(a, 0))
            set(a, "levels16OriginalPad", 7)
            val tune =
                TrainerActivity::class
                    .java
                    .getDeclaredMethod("levels16TuneSemitones", Int::class.javaPrimitiveType)
                    .apply { isAccessible = true }
            assertEquals(0, tune.invoke(a, 7))
            assertEquals(4, tune.invoke(a, 11))
        } finally {
            c.pause().stop().destroy()
        }
    }
}

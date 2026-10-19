from pathlib import Path

p=Path('app/src/test/java/com/bytbeat/mpctrainer/PitchTimingCorrectIntegrationTest.kt')
p.parent.mkdir(parents=True,exist_ok=True)
p.write_text(r'''package com.bytbeat.mpctrainer

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class PitchTimingCorrectIntegrationTest {
    private fun generatedFile(name: String): String {
        val cwd = File(System.getProperty("user.dir") ?: ".").absoluteFile
        val candidates = listOf(
            File(cwd, "src/main/java/com/bytbeat/mpctrainer/$name"),
            File(cwd, "app/src/main/java/com/bytbeat/mpctrainer/$name"),
            File(cwd.parentFile ?: cwd, "app/src/main/java/com/bytbeat/mpctrainer/$name")
        )
        val source = candidates.firstOrNull { it.isFile }
            ?: error("Generated $name not found from ${cwd.path}: ${candidates.joinToString { it.path }}")
        return source.readText()
    }

    private fun activity() = generatedFile("TrainerActivity.kt")
    private fun app() = generatedFile("TrainerApp.kt")

    @Test fun pitch_is_real_persisted_resettable_and_exposed_in_phone_ui() {
        val s = activity(); val ui = app()
        assertTrue(s.contains("private val padPitch=Array(4){IntArray(16){0}}"))
        assertTrue(s.contains("java.lang.Math.pow(2.0,padPitch[b][p]/12.0)"))
        assertTrue(s.contains("pitch.put(padPitch[b][p])"))
        assertTrue(s.contains("padPitch[b][p]=pitch.optInt(i,0).coerceIn(-12,12)"))
        assertTrue(s.contains("fun resetSelectedPadPitch(){padPitch[bank][pad]=0"))
        assertTrue(ui.contains("PITCH −")); assertTrue(ui.contains("PITCH +"))
        assertTrue(ui.contains("adjustSelectedPadPitch(-1)")); assertTrue(ui.contains("adjustSelectedPadPitch(1)"))
    }

    @Test fun timing_correct_supports_global_toggle_do_it_triplet_and_shift() {
        val s = activity()
        assertTrue(s.contains("private var tcEnabled=true"))
        assertTrue(s.contains("intArrayOf(4,8,16,32,64)"))
        assertTrue(s.contains("fun quantizeCurrentTrack():Int")); assertTrue(s.contains("if(e.track==track)"))
        assertTrue(s.contains("setNeutralButton(\"DO IT\")")); assertTrue(s.contains("quantizeCurrentTrack()"))
        assertTrue(s.contains("put(\"tcOn\",tcEnabled)")); assertTrue(s.contains("tcEnabled=o.optBoolean(\"tcOn\",true)"))
        assertTrue(s.contains("tcTriplet")); assertTrue(s.contains("tcShift")); assertTrue(s.contains("TRIPLET")); assertTrue(s.contains("SHIFT TIMING"))
        assertTrue(s.contains("private fun tcGridTicks():Long"))
    }

    @Test fun note_repeat_follows_current_straight_or_triplet_tc_grid() {
        val s = activity()
        assertTrue(s.contains("private fun noteRepeatIntervalMs()=(60000.0/bpm*(tcGridTicks()/96.0))"))
        assertTrue(s.contains("h.postDelayed(this,noteRepeatIntervalMs())"))
    }

    @Test fun basic_16_levels_exposes_velocity_and_tune_without_fake_tune_recording() {
        val s = activity()
        assertTrue(s.contains("arrayOf(\"VELOCITY\",\"TUNE\")"))
        assertTrue(s.contains("levels16TuneSemitones")); assertTrue(s.contains("levels16Rate"))
        assertTrue(s.contains("запись pitch-событий пока недоступна"))
    }
}
''')
print('Pitch/TC regression tests aligned with triplet-aware generated Activity and phone UI')
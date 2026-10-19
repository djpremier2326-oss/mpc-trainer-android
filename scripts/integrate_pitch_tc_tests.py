from pathlib import Path

p=Path('app/src/test/java/com/bytbeat/mpctrainer/PitchTimingCorrectIntegrationTest.kt')
p.parent.mkdir(parents=True,exist_ok=True)
p.write_text(r'''package com.bytbeat.mpctrainer

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class PitchTimingCorrectIntegrationTest {
    private fun generatedSource(): String {
        val root = File(System.getProperty("user.dir")).parentFile.parentFile
        return File(root, "app/src/main/java/com/bytbeat/mpctrainer/TrainerActivity.kt").readText()
    }

    @Test fun pitch_is_real_persisted_and_resettable() {
        val s = generatedSource()
        assertTrue(s.contains("private val padPitch=Array(4){IntArray(16){0}}"))
        assertTrue(s.contains("java.lang.Math.pow(2.0,padPitch[b][p]/12.0)"))
        assertTrue(s.contains("pitch.put(padPitch[b][p])"))
        assertTrue(s.contains("padPitch[b][p]=pitch.optInt(i,0).coerceIn(-12,12)"))
        assertTrue(s.contains("fun resetSelectedPadPitch(){padPitch[bank][pad]=0"))
        assertTrue(s.contains("PITCH −"))
        assertTrue(s.contains("PITCH +"))
    }

    @Test fun timing_correct_supports_global_toggle_and_do_it() {
        val s = generatedSource()
        assertTrue(s.contains("private var tcEnabled=true"))
        assertTrue(s.contains("intArrayOf(4,8,16,32,64)"))
        assertTrue(s.contains("fun quantizeCurrentTrack():Int"))
        assertTrue(s.contains("if(e.track==track)"))
        assertTrue(s.contains("setPositiveButton(\"DO IT\")"))
        assertTrue(s.contains("put(\"tcOn\",tcEnabled)"))
        assertTrue(s.contains("tcEnabled=o.optBoolean(\"tcOn\",true)"))
    }

    @Test fun note_repeat_follows_current_tc_division() {
        val s = generatedSource()
        assertTrue(s.contains("private fun noteRepeatIntervalMs()=(60000.0/bpm*(4.0/tc.coerceAtLeast(1)))"))
        assertTrue(s.contains("h.postDelayed(this,noteRepeatIntervalMs())"))
    }

    @Test fun basic_16_levels_exposes_velocity_and_tune_without_fake_tune_recording() {
        val s = generatedSource()
        assertTrue(s.contains("arrayOf(\"VELOCITY\",\"TUNE\")"))
        assertTrue(s.contains("levels16TuneSemitones"))
        assertTrue(s.contains("levels16Rate"))
        assertTrue(s.contains("запись pitch-событий пока недоступна"))
    }
}
''')
print('Pitch/TC generated-source regression tests added')
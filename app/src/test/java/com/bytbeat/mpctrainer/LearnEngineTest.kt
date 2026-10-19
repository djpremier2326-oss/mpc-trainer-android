package com.bytbeat.mpctrainer

import org.junit.Assert.*
import org.junit.Test

class LearnEngineTest {
    private fun s(
        pads:Int=0, events:Int=0, track:Int=0, muted:Int=0,
        tc:Int=16, swing:Int=50, repeat:Boolean=false,
        full:Boolean=false, levels:Boolean=false
    ) = LearnEngine.Snapshot(pads,events,track,muted,tc,swing,repeat,full,levels)

    @Test fun lessonsRequireRealStateChanges() {
        val e=LearnEngine(1)
        e.begin(s())
        assertFalse(e.report("loaded",s()).advanced)
        assertTrue(e.report("loaded",s(pads=1)).advanced)
        assertEquals(2,e.lesson)
        assertTrue(e.report("pad",s(pads=1)).advanced)

        e.begin(s(pads=1,events=2))
        assertFalse(e.report("stop",s(pads=1,events=2)).advanced)
        assertTrue(e.report("stop",s(pads=1,events=3)).advanced)
    }

    @Test fun performanceLessonsNeedPadUse() {
        val repeat=LearnEngine(7); repeat.begin(s(pads=1))
        assertFalse(repeat.report("note_repeat",s(pads=1,repeat=true)).advanced)
        assertTrue(repeat.report("note_repeat_pad",s(pads=1,repeat=true)).advanced)

        val full=LearnEngine(8); full.begin(s(pads=1))
        assertFalse(full.report("full_level",s(pads=1,full=true)).advanced)
        assertTrue(full.report("full_level_pad",s(pads=1,full=true)).advanced)

        val levels=LearnEngine(9); levels.begin(s(pads=1))
        assertTrue(levels.report("16_levels_pad",s(pads=1,levels=true)).advanced)
    }

    @Test fun sampleEditRequiresNewPadAssignments() {
        val e=LearnEngine(13); e.begin(s(pads=1))
        assertFalse(e.report("chopped",s(pads=1)).advanced)
        assertTrue(e.report("chopped",s(pads=5)).advanced)
    }

    @Test fun challengeNeedsPadsRecordingStepAndSave() {
        val e=LearnEngine(LearnEngine.LAST_LESSON); e.begin(s())
        var r=e.report("stop",s(pads=2,events=2))
        assertTrue(r.message.contains("○ Step Seq"))
        r=e.report("step_added",s(pads=2,events=3))
        assertTrue(r.message.contains("○ SAVE"))
        r=e.report("save",s(pads=2,events=3))
        assertTrue(r.message.contains("FOUNDATION COMPLETE"))
    }
}

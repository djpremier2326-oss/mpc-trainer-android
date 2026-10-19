package com.bytbeat.mpctrainer

import org.junit.Assert.*
import org.junit.Test

class LearnEngineTest {
    private fun s(pads:Int=0,events:Int=0,track:Int=0,muted:Int=0,tc:Int=16,swing:Int=50,repeat:Boolean=false,full:Boolean=false,levels:Boolean=false)=LearnEngine.Snapshot(pads,events,track,muted,tc,swing,repeat,full,levels)

    @Test fun lessonsRequireRealStateChanges(){val e=LearnEngine(1);e.begin(s());assertFalse(e.report("loaded",s()).advanced);assertTrue(e.report("loaded",s(pads=1)).advanced);assertEquals(2,e.lesson);assertTrue(e.report("pad",s(pads=1)).advanced);e.begin(s(pads=1,events=2));assertFalse(e.report("stop",s(pads=1,events=2)).advanced);assertTrue(e.report("stop",s(pads=1,events=3)).advanced)}

    @Test fun performanceLessonsNeedPadUse(){val repeat=LearnEngine(7);repeat.begin(s(pads=1));assertFalse(repeat.report("note_repeat",s(pads=1,repeat=true)).advanced);assertTrue(repeat.report("note_repeat_pad",s(pads=1,repeat=true)).advanced);val full=LearnEngine(8);full.begin(s(pads=1));assertFalse(full.report("full_level",s(pads=1,full=true)).advanced);assertTrue(full.report("full_level_pad",s(pads=1,full=true)).advanced);val levels=LearnEngine(9);levels.begin(s(pads=1));assertTrue(levels.report("16_levels_pad",s(pads=1,levels=true)).advanced)}

    @Test fun sampleEditAcceptsSuccessfulReplacement(){val e=LearnEngine(13);e.begin(s(pads=1));assertFalse(e.report("chopped",s()).advanced);assertTrue(e.report("chopped",s(pads=1)).advanced)}

    @Test fun mixerLessonRequiresAppliedChange(){val e=LearnEngine(14);e.begin(s(pads=2));assertFalse(e.report("menu",s(pads=2)).advanced);assertTrue(e.report("mixer_changed",s(pads=2)).advanced);assertEquals(LearnEngine.LAST_LESSON,e.lesson)}

    @Test fun challengeRequiresCoreWorkflowFromCleanProject(){val e=LearnEngine(LearnEngine.LAST_LESSON);e.begin(s());var r=e.report("stop",s(pads=2,events=2));assertTrue(r.message.contains("✓ чистый проект"));assertTrue(r.message.contains("✓ REC"));assertTrue(r.message.contains("○ OVERDUB"));r=e.report("step_added",s(pads=2,events=3));r=e.report("overdub_stop",s(pads=2,events=4));r=e.report("tc",s(pads=2,events=4,tc=32));r=e.report("track",s(pads=2,events=4,track=1,tc=32));r=e.report("mixer_changed",s(pads=2,events=4,track=1,tc=32));r=e.report("save",s(pads=2,events=4,track=1,tc=32));assertTrue(r.message.contains("ОСНОВЫ ПРОЙДЕНЫ"))}

    @Test fun challengeDoesNotFinishWithoutBalance(){val e=LearnEngine(LearnEngine.LAST_LESSON);e.begin(s());e.report("stop",s(pads=2,events=2));e.report("overdub_stop",s(pads=2,events=3));e.report("tc",s(pads=2,events=3,tc=32));e.report("track",s(pads=2,events=3,track=1,tc=32));e.report("step_added",s(pads=2,events=4,track=1,tc=32));val r=e.report("save",s(pads=2,events=4,track=1,tc=32));assertFalse(r.message.contains("ОСНОВЫ ПРОЙДЕНЫ"));assertTrue(r.message.contains("○ баланс"))}

    @Test fun dirtyChallengeCannotReuseLessonWork(){val e=LearnEngine(LearnEngine.LAST_LESSON);e.begin(s(pads=5,events=20,tc=16,track=0));var r=e.report("stop",s(pads=5,events=21));r=e.report("overdub_stop",s(pads=5,events=22));r=e.report("tc",s(pads=5,events=22,tc=32));r=e.report("track",s(pads=5,events=22,track=1,tc=32));r=e.report("step_added",s(pads=5,events=23,track=1,tc=32));r=e.report("mixer_changed",s(pads=5,events=23,track=1,tc=32));r=e.report("save",s(pads=5,events=23,track=1,tc=32));assertFalse(r.message.contains("ОСНОВЫ ПРОЙДЕНЫ"));assertTrue(r.message.contains("○ чистый проект"));r=e.report("new_project",s());assertTrue(r.message.contains("✓ чистый проект"));assertTrue(r.message.contains("○ REC"))}

    @Test fun challengeTcAndTrackMustActuallyChange(){val e=LearnEngine(LearnEngine.LAST_LESSON);e.begin(s(tc=16,track=0));var r=e.report("tc",s(pads=2,events=2,tc=16,track=0));assertTrue(r.message.contains("○ TC"));r=e.report("track",s(pads=2,events=2,tc=16,track=0));assertTrue(r.message.contains("○ другой TRACK"))}

    @Test fun courseCopyIsRussianWhileHardwareLabelsStayCanonical(){val browse=LearnEngine(1);assertTrue(browse.explanation().contains("поиска"));assertTrue(browse.prompt().contains("BROWSE"));val record=LearnEngine(3);assertTrue(record.explanation().contains("запись"));assertTrue(record.prompt().contains("REC"));val mixer=LearnEngine(14);assertTrue(mixer.explanation().contains("баланса"));assertTrue(mixer.prompt().contains("MIXER / VOLUME"));val challenge=LearnEngine(LearnEngine.LAST_LESSON);assertTrue(challenge.prompt().contains("ЭКЗАМЕН"));assertTrue(challenge.prompt().contains("NEW PROJECT"))}
}

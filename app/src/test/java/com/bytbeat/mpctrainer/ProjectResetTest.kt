package com.bytbeat.mpctrainer

import org.junit.Assert.*
import org.junit.Test

class ProjectResetTest {
    @Test fun resetCoversProjectMixerAndEveryPadAssignmentOnly() {
        val keys=ProjectReset.preferenceKeys()
        assertEquals(130,keys.size)
        assertEquals(keys.size,keys.toSet().size)
        assertTrue("project" in keys)
        assertTrue("volumes" in keys)
        for(b in 0..3) for(p in 0..15){
            assertTrue("p_${b}_$p" in keys)
            assertTrue("n_${b}_$p" in keys)
        }
        assertFalse(keys.any{it.contains("file",true)||it.contains("sample_edit",true)||it.contains("audio",true)})
    }
}

package com.bytbeat.mpctrainer

import org.junit.Assert.*
import org.junit.Test

class ProjectResetTest {
    @Test fun resetCoversCurrentProjectStateWithoutDeletingAudioFiles() {
        val keys=ProjectReset.preferenceKeys()
        assertEquals(3,keys.size)
        assertEquals(keys.size,keys.toSet().size)
        assertTrue("project" in keys)
        assertTrue("volumes" in keys)
        assertTrue("pads" in keys)
        assertFalse(keys.any{it.contains("file",true)||it.contains("sample_edit",true)||it.contains("audio",true)})
        assertFalse(keys.any{it.startsWith("p_")||it.startsWith("n_")})
    }
}

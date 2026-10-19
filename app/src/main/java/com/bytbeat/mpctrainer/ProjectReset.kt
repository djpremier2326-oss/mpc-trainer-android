package com.bytbeat.mpctrainer

/** Defines only trainer state keys that belong to a project. Source audio files are never deleted here. */
object ProjectReset {
    fun preferenceKeys(): List<String> = buildList {
        add("project")
        add("volumes")
        for (bank in 0..3) for (pad in 0..15) {
            add("p_${bank}_$pad")
            add("n_${bank}_$pad")
        }
    }
}

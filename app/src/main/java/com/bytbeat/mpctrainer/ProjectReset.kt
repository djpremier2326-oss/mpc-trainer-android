package com.bytbeat.mpctrainer

/** Defines only trainer state keys that belong to a project. Source audio files are never deleted here. */
object ProjectReset {
    fun preferenceKeys(): List<String> = listOf(
        "project",
        "volumes",
        "pads"
    )
}

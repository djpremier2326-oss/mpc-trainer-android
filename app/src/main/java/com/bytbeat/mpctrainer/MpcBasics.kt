package com.bytbeat.mpctrainer

data class ControlGuide(
    val name: String,
    val purpose: String,
    val practice: String
)

data class BasicLesson(
    val title: String,
    val goal: String,
    val controls: List<String>
)

/**
 * Scope for the phone trainer: teach the controls and the normal beat-making
 * workflow first. Deep MIDI, automation and detailed note editing are
 * deliberately outside the foundation course.
 */
object MpcBasics {
    val controls = listOf(
        ControlGuide("MAIN", "Returns to the main working view: sequence, track and transport context.", "Return here after Browser or editing."),
        ControlGuide("BROWSE", "Finds and auditions sounds, then loads a sound to a selected pad.", "Choose a drum sound, preview it and load it to a pad."),
        ControlGuide("BANK A–D", "Switches between groups of 16 pads.", "Load sounds in Bank A, then switch banks without losing assignments."),
        ControlGuide("PADS 1–16", "Play assigned sounds and enter drum events.", "Play kick, snare and hi-hat from pads."),
        ControlGuide("REC", "Starts a new recording pass on the current track.", "Record a simple two-bar drum pattern."),
        ControlGuide("OVERDUB", "Adds new pad hits without replacing the existing pattern.", "Add hi-hats over a kick/snare pattern."),
        ControlGuide("PLAY / STOP", "Starts and stops sequence playback.", "Listen back and stop on command."),
        ControlGuide("TIMING CORRECT", "Quantizes recorded timing to a rhythmic grid.", "Compare a pattern before and after 1/16 timing correction."),
        ControlGuide("SWING", "Offsets alternating timing positions to change groove feel.", "Hear the same pattern at straight and swung timing."),
        ControlGuide("NOTE REPEAT", "Repeats a held pad at the selected rhythmic division.", "Hold a hi-hat pad and create an even repeat."),
        ControlGuide("FULL LEVEL", "Makes pad hits play at maximum velocity.", "Toggle it and compare level with a normal trainer pad hit."),
        ControlGuide("16 LEVELS", "Maps one selected pad across 16 graduated levels; foundation course uses velocity.", "Select one sound and hear its velocity rise across the 16 pads."),
        ControlGuide("STEP SEQ", "Programs a rhythm one step at a time using 16 steps per bar.", "Place kick and snare steps and move between bars."),
        ControlGuide("TRACK", "Selects the current musical/drum track inside the sequence.", "Put a second part on another track."),
        ControlGuide("TRACK MUTE", "Temporarily silences a track without deleting it.", "Mute and unmute a layer during playback."),
        ControlGuide("SAMPLE EDIT", "Basic trainer use: inspect a sample, set useful start/end points and make simple chops.", "Trim a sample and make a few pad-ready slices."),
        ControlGuide("SAVE / PROJECT", "Keeps pad assignments and the beat so work can be resumed.", "Close and reopen the trainer without losing the exercise.")
    )

    val lessons = listOf(
        BasicLesson("1. Know the surface", "Recognize the main Live III-style control groups and explain what each basic button does.", listOf("MAIN", "BROWSE", "BANK A–D", "PADS 1–16", "REC", "OVERDUB", "PLAY / STOP")),
        BasicLesson("2. Browser to pads", "Find, preview and assign kick, snare and hi-hat sounds.", listOf("BROWSE", "BANK A–D", "PADS 1–16")),
        BasicLesson("3. First sequence", "Record and play back a simple two-bar beat.", listOf("REC", "PLAY / STOP", "PADS 1–16")),
        BasicLesson("4. Add a layer", "Add hi-hats without erasing the first pass.", listOf("OVERDUB", "NOTE REPEAT")),
        BasicLesson("5. Timing and groove", "Understand quantize and basic swing by hearing the result.", listOf("TIMING CORRECT", "SWING")),
        BasicLesson("6. Pad performance tools", "Understand Full Level and velocity-based 16 Levels.", listOf("FULL LEVEL", "16 LEVELS")),
        BasicLesson("7. Step sequencing", "Build and correct a basic pattern on the 16-step grid.", listOf("STEP SEQ")),
        BasicLesson("8. Tracks", "Use a second track and understand Track Mute.", listOf("TRACK", "TRACK MUTE")),
        BasicLesson("9. Basic sample work", "Learn only practical start/end and simple chopping needed for beatmaking.", listOf("SAMPLE EDIT")),
        BasicLesson("10. Build a beat alone", "Start from an empty project and finish a playable saved beat without hints.", listOf("BROWSE", "PADS 1–16", "REC", "OVERDUB", "TIMING CORRECT", "STEP SEQ", "SAVE / PROJECT"))
    )

    val intentionallyAdvanced = listOf(
        "Deep MIDI routing and configuration",
        "Detailed note-by-note MIDI editing",
        "Advanced automation editing",
        "Probability, ratchet and micro-nudge workflows",
        "Advanced Q-Link mapping",
        "Deep synthesis and plugin programming"
    )
}

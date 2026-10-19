package com.bytbeat.mpctrainer

/**
 * State-based foundation course. The UI reports real actions; lessons advance
 * only after the requested workflow changes actual trainer state.
 */
class LearnEngine(startLesson: Int = 0) {
    data class Snapshot(
        val loadedPads: Int = 0,
        val eventCount: Int = 0,
        val currentTrack: Int = 0,
        val mutedTracks: Int = 0,
        val tc: Int = 16,
        val swing: Int = 50,
        val noteRepeat: Boolean = false,
        val fullLevel: Boolean = false,
        val levels16: Boolean = false
    )

    data class Result(val advanced: Boolean, val lesson: Int, val message: String)

    var lesson: Int = startLesson.coerceIn(0, LAST_LESSON)
        private set

    private var baselineEvents = 0
    private var baselineTrack = 0
    private var baselineSwing = 50

    fun begin(s: Snapshot) {
        baselineEvents = s.eventCount
        baselineTrack = s.currentTrack
        baselineSwing = s.swing
    }

    fun reset(s: Snapshot): Result {
        lesson = 0
        begin(s)
        return Result(false, lesson, prompt())
    }

    fun report(action: String, s: Snapshot): Result {
        val complete = when (lesson) {
            0 -> action == "main"
            1 -> action == "loaded" && s.loadedPads > 0
            2 -> action == "pad" && s.loadedPads > 0
            3 -> action == "stop" && s.eventCount > baselineEvents
            4 -> action == "overdub_stop" && s.eventCount > baselineEvents
            5 -> action == "tc" && s.tc in setOf(8, 16, 32)
            6 -> action == "swing" && s.swing != baselineSwing
            7 -> action == "note_repeat" && s.noteRepeat
            8 -> action == "full_level" && s.fullLevel
            9 -> action == "16_levels" && s.levels16
            10 -> action == "step_added"
            11 -> action == "track" && s.currentTrack != baselineTrack
            12 -> action == "track_mute" && s.mutedTracks > 0
            else -> false
        }
        return if (complete) advance(s) else Result(false, lesson, hint(action))
    }

    private fun advance(s: Snapshot): Result {
        if (lesson < LAST_LESSON) lesson++
        begin(s)
        return Result(true, lesson, prompt())
    }

    fun title(): String = when (lesson) {
        0 -> "1 • MAIN"
        1 -> "2 • Browser"
        2 -> "3 • Pads"
        3 -> "4 • Record"
        4 -> "5 • Overdub"
        5 -> "6 • Timing Correct"
        6 -> "7 • Swing"
        7 -> "8 • Note Repeat"
        8 -> "9 • Full Level"
        9 -> "10 • 16 Levels"
        10 -> "11 • Step Sequencer"
        11 -> "12 • Tracks"
        12 -> "13 • Track Mute"
        else -> "Foundation Challenge"
    }

    fun explanation(): String = when (lesson) {
        0 -> "MAIN returns you to the main working view: sequence, track, bank, tempo and transport context."
        1 -> "BROWSE is where you find and audition sounds, then load the chosen sound onto a pad."
        2 -> "The 16 pads play assigned sounds. Banks A–D give four groups of 16 pad slots."
        3 -> "REC starts a new recording pass on the current track. In this trainer a fresh REC pass replaces that track's previous events."
        4 -> "OVERDUB adds new hits while keeping the events already recorded on the track."
        5 -> "Timing Correct (TC) aligns recorded hits to a rhythmic grid such as 1/8, 1/16 or 1/32."
        6 -> "Swing shifts alternating grid positions to change the groove from straight toward a swung feel."
        7 -> "NOTE REPEAT repeatedly triggers a held pad at the selected rhythmic division."
        8 -> "FULL LEVEL makes pad hits play at maximum velocity. The phone trainer uses fixed velocity rather than pressure-sensitive pads."
        9 -> "16 LEVELS spreads one selected sound across all 16 pads. The foundation course teaches the velocity version: low to high."
        10 -> "STEP SEQ programs rhythm one step at a time. The trainer shows 16 steps per bar and lets you move between bars."
        11 -> "TRACK selects a separate part inside the current sequence, useful for keeping layers independent."
        12 -> "TRACK MUTE silences a track without deleting its recorded events."
        else -> "Use the controls without step-by-step hints and build a basic beat from an empty project."
    }

    fun prompt(): String = when (lesson) {
        0 -> "TASK • Press MAIN."
        1 -> "TASK • BROWSE → choose a sound → preview it → LOAD TO PAD."
        2 -> "TASK • Hit a loaded pad."
        3 -> "TASK • Press REC, play a short pattern, then STOP."
        4 -> "TASK • Press OVERDUB, add another layer, then STOP."
        5 -> "TASK • Change the Timing Correct division."
        6 -> "TASK • Change Swing from its current value."
        7 -> "TASK • Turn NOTE REPEAT on, then hold a loaded pad."
        8 -> "TASK • Turn FULL LEVEL on."
        9 -> "TASK • Select a loaded pad and turn 16 LEVELS on."
        10 -> "TASK • Open STEP SEQ and add a new step."
        11 -> "TASK • Change to another TRACK."
        12 -> "TASK • Use TRACK MUTE on the selected track."
        else -> "CHALLENGE • Build and save a playable beat without guided steps."
    }

    fun lessonCard(): String = "${title()}\n${explanation()}\n\n${prompt()}"

    private fun hint(action: String): String = when (lesson) {
        1 -> if (action == "browse") "Browser is open. Choose a sound, preview it, then LOAD TO PAD." else prompt()
        3 -> if (action == "record") "REC is active. Play at least one pad, then STOP." else prompt()
        4 -> if (action == "overdub") "OVERDUB is active. Add at least one new hit, then STOP." else prompt()
        7 -> if (action == "note_repeat") "NOTE REPEAT is on. Hold a loaded pad to hear the repeated trigger." else prompt()
        10 -> "Opening STEP SEQ is not enough: add a step to the grid."
        else -> prompt()
    }

    companion object { const val LAST_LESSON = 13 }
}

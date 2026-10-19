package com.bytbeat.mpctrainer

/**
 * Small state machine for the phone trainer. UI reports real actions and the
 * engine advances only when the requested MPC workflow was actually performed.
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

    data class Result(
        val advanced: Boolean,
        val lesson: Int,
        val message: String
    )

    var lesson: Int = startLesson.coerceIn(0, LAST_LESSON)
        private set

    private var baselineEvents = 0
    private var baselineTrack = 0
    private var baselineSwing = 50
    private var stepAdded = false

    fun begin(snapshot: Snapshot) {
        baselineEvents = snapshot.eventCount
        baselineTrack = snapshot.currentTrack
        baselineSwing = snapshot.swing
        stepAdded = false
    }

    fun reset(snapshot: Snapshot): Result {
        lesson = 0
        begin(snapshot)
        return Result(false, lesson, prompt())
    }

    fun report(action: String, s: Snapshot): Result {
        val complete = when (lesson) {
            0 -> action == "loaded" && s.loadedPads > 0
            1 -> action == "pad" && s.loadedPads > 0
            2 -> action == "stop" && s.eventCount > baselineEvents
            3 -> action == "overdub_stop" && s.eventCount > baselineEvents
            4 -> (action == "tc" && s.tc in setOf(8,16,32)) ||
                 (action == "swing" && s.swing != baselineSwing)
            5 -> action == "note_repeat" && s.noteRepeat
            6 -> action == "full_level" && s.fullLevel
            7 -> action == "16_levels" && s.levels16
            8 -> action == "step" && stepAdded
            9 -> action == "track" && s.currentTrack != baselineTrack
            10 -> action == "track_mute" && s.mutedTracks > 0
            else -> false
        }

        if (action == "step") stepAdded = true
        if (!complete && lesson == 8 && action == "step") return advance(s)
        if (complete) return advance(s)

        return Result(false, lesson, hint(action))
    }

    private fun advance(s: Snapshot): Result {
        if (lesson < LAST_LESSON) lesson++
        begin(s)
        return Result(true, lesson, prompt())
    }

    fun prompt(): String = when (lesson) {
        0 -> "BROWSER → choose a sound → preview it → LOAD TO PAD."
        1 -> "Hit the pad you just loaded. Pads play sounds and enter events."
        2 -> "Press REC, play a short pattern, then STOP."
        3 -> "Press OVERDUB, add another layer, then STOP. Existing notes must remain."
        4 -> "Use TC / SWING. Learn that TC aligns timing and Swing changes the groove feel."
        5 -> "Turn NOTE REPEAT on, then hold a loaded pad."
        6 -> "Turn FULL LEVEL on. It forces maximum velocity in this trainer."
        7 -> "Select a loaded pad and turn 16 LEVELS on. The foundation lesson uses velocity levels."
        8 -> "Open STEP SEQ and add a new step."
        9 -> "Change to another TRACK. Tracks separate parts inside the sequence."
        10 -> "Use TRACK MUTE to silence the selected track without deleting it."
        else -> "FOUNDATION PRACTICE: build a beat from an empty project without guided steps."
    }

    fun title(): String = when (lesson) {
        0 -> "1 • Browser"
        1 -> "2 • Pads"
        2 -> "3 • Record"
        3 -> "4 • Overdub"
        4 -> "5 • Timing & Swing"
        5 -> "6 • Note Repeat"
        6 -> "7 • Full Level"
        7 -> "8 • 16 Levels"
        8 -> "9 • Step Sequencer"
        9 -> "10 • Tracks"
        10 -> "11 • Track Mute"
        else -> "Foundation Challenge"
    }

    private fun hint(action: String): String = when (lesson) {
        0 -> if (action == "browse") "Good. Now choose a sound and LOAD TO PAD." else prompt()
        2 -> if (action == "record") "Recording is armed. Play the pad, then press STOP." else prompt()
        3 -> if (action == "overdub") "Overdub is active. Add hits, then press STOP." else prompt()
        8 -> "A step must actually be added to the grid; simply opening STEP SEQ is not enough."
        else -> prompt()
    }

    companion object { const val LAST_LESSON = 11 }
}

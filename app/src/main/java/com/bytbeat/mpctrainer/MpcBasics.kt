package com.bytbeat.mpctrainer

data class ControlGuide(val name:String,val purpose:String,val practice:String)
data class BasicLesson(val title:String,val goal:String,val controls:List<String>)

object MpcBasics {
 val controls=listOf(
  ControlGuide("MAIN","Returns to the main working view: sequence, track and transport context.","Return here after Browser or Sample Edit."),
  ControlGuide("BROWSE","Finds and auditions WAV/MP3 sounds, then loads a sound to the selected pad.","Choose a sound, preview it and load it to a pad."),
  ControlGuide("BANK A–D","Switches between groups of 16 pads.","Load sounds in Bank A, then switch banks without losing assignments."),
  ControlGuide("PADS 1–16","Play assigned sounds and enter drum events.","Play kick, snare and hi-hat from pads."),
  ControlGuide("BPM / TEMPO","Sets project tempo. Tempo drives playback, recording, metronome and Note Repeat timing.","Set a slow BPM, follow the metronome, then compare with a faster BPM."),
  ControlGuide("REC","Starts a new recording pass on the current track.","Record a simple two-bar drum pattern."),
  ControlGuide("OVERDUB","Adds new pad hits without replacing the existing pattern.","Add hi-hats over a kick/snare pattern."),
  ControlGuide("PLAY / STOP","Starts and stops sequence playback.","Listen back and stop on command."),
  ControlGuide("METRONOME","Provides a tempo reference while recording or playing.","Turn it on and follow the click at the project BPM."),
  ControlGuide("COUNT IN","Gives a four-beat lead-in before recording begins.","Enable it, press REC and enter after the count."),
  ControlGuide("TIMING CORRECT","Quantizes recorded timing to a rhythmic grid.","Compare 1/8, 1/16 and 1/32 grid choices."),
  ControlGuide("SWING","Offsets alternating timing positions to change groove feel.","Hear the same pattern straight and with swing."),
  ControlGuide("NOTE REPEAT","Repeats a held pad at the selected rhythmic division.","Hold a hi-hat pad and create an even repeat."),
  ControlGuide("FULL LEVEL","Makes pad hits play at maximum velocity.","Toggle it and compare with a normal trainer pad hit."),
  ControlGuide("16 LEVELS","Maps one selected pad across 16 graduated velocity levels in this foundation trainer.","Select one sound and hear velocity rise across the pads."),
  ControlGuide("STEP SEQ","Programs rhythm one step at a time.","Place kick and snare steps on the grid."),
  ControlGuide("TRACK","Selects the current part inside the sequence.","Put a second layer on another track."),
  ControlGuide("TRACK MUTE","Silences a track without deleting it.","Mute and unmute a layer during playback."),
  ControlGuide("SAMPLE EDIT","Takes a loaded WAV or MP3, sets Start/End and makes simple even chops. MP3 is decoded internally for editing.","Load a musical MP3, choose the useful range, chop it and play the slices from consecutive pads."),
  ControlGuide("SAVE / PROJECT","Keeps pad assignments, recorded events and key project settings for the next launch.","Save, close and reopen the trainer to resume the exercise."),
  ControlGuide("NEW PROJECT","Returns the trainer to an empty project while leaving imported source audio files intact on the phone.","Use it before the final challenge so you build the beat from zero.")
 )
 val lessons=listOf(
  BasicLesson("1. Know the surface","Recognize the main control groups and explain the foundation buttons.",listOf("MAIN","BROWSE","BANK A–D","PADS 1–16","BPM / TEMPO","REC","OVERDUB","PLAY / STOP")),
  BasicLesson("2. Browser to pads","Find, preview and assign sounds.",listOf("BROWSE","BANK A–D","PADS 1–16")),
  BasicLesson("3. First sequence","Set tempo, record and play back a simple two-bar beat.",listOf("BPM / TEMPO","COUNT IN","METRONOME","REC","PLAY / STOP","PADS 1–16")),
  BasicLesson("4. Add a layer","Add hi-hats without erasing the first pass.",listOf("OVERDUB","NOTE REPEAT")),
  BasicLesson("5. Timing and groove","Understand quantize and swing by hearing the result.",listOf("TIMING CORRECT","SWING")),
  BasicLesson("6. Pad performance tools","Understand Full Level and velocity-based 16 Levels.",listOf("FULL LEVEL","16 LEVELS")),
  BasicLesson("7. Step sequencing","Build and correct a basic pattern on the step grid.",listOf("STEP SEQ")),
  BasicLesson("8. Tracks","Use another track and understand Track Mute.",listOf("TRACK","TRACK MUTE")),
  BasicLesson("9. Basic sample work","Load WAV/MP3, set Start/End and chop a musical sample to pads.",listOf("BROWSE","SAMPLE EDIT")),
  BasicLesson("10. Build a beat alone","Start from an empty project and finish a playable saved beat without hints.",listOf("NEW PROJECT","BROWSE","PADS 1–16","BPM / TEMPO","REC","OVERDUB","TIMING CORRECT","TRACK","STEP SEQ","SAVE / PROJECT"))
 )
 val intentionallyAdvanced=listOf("Deep MIDI routing and configuration","Detailed note-by-note MIDI editing","Advanced automation editing","Probability, ratchet and micro-nudge workflows","Advanced Q-Link mapping","Deep synthesis and plugin programming")
}

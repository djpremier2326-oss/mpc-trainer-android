package com.bytbeat.mpctrainer

/** State-based foundation course driven by real trainer actions. */
class LearnEngine(startLesson: Int = 0) {
 data class Snapshot(val loadedPads:Int=0,val eventCount:Int=0,val currentTrack:Int=0,val mutedTracks:Int=0,val tc:Int=16,val swing:Int=50,val noteRepeat:Boolean=false,val fullLevel:Boolean=false,val levels16:Boolean=false)
 data class Result(val advanced:Boolean,val lesson:Int,val message:String)
 var lesson:Int=startLesson.coerceIn(0,LAST_LESSON);private set
 private var baselineEvents=0;private var baselineTrack=0;private var baselineSwing=50;private var baselineTc=16;private var baselineMute=0;private var baselinePads=0
 fun begin(s:Snapshot){baselineEvents=s.eventCount;baselineTrack=s.currentTrack;baselineSwing=s.swing;baselineTc=s.tc;baselineMute=s.mutedTracks;baselinePads=s.loadedPads}
 fun reset(s:Snapshot):Result{lesson=0;begin(s);return Result(false,lesson,prompt())}
 fun report(action:String,s:Snapshot):Result{val complete=when(lesson){
  0->action=="main"
  1->action=="loaded"&&s.loadedPads>baselinePads
  2->action=="loaded_pad_hit"
  3->action=="stop"&&s.eventCount>baselineEvents
  4->action=="overdub_stop"&&s.eventCount>baselineEvents
  5->action=="tc"&&s.tc!=baselineTc
  6->action=="swing"&&s.swing!=baselineSwing
  7->action=="note_repeat_used"
  8->action=="full_level"&&s.fullLevel
  9->action=="16_levels"&&s.levels16
  10->action=="step_added"
  11->action=="track"&&s.currentTrack!=baselineTrack
  12->action=="track_mute"&&s.mutedTracks!=baselineMute
  13->action=="sample_chopped"&&s.loadedPads>baselinePads
  14->action=="project_saved"
  else->false};return if(complete)advance(s)else Result(false,lesson,hint(action))}
 private fun advance(s:Snapshot):Result{if(lesson<LAST_LESSON)lesson++;begin(s);return Result(true,lesson,prompt())}
 fun title()=when(lesson){0->"1 • MAIN";1->"2 • Browser";2->"3 • Pads";3->"4 • Record";4->"5 • Overdub";5->"6 • Timing Correct";6->"7 • Swing";7->"8 • Note Repeat";8->"9 • Full Level";9->"10 • 16 Levels";10->"11 • Step Sequencer";11->"12 • Tracks";12->"13 • Track Mute";13->"14 • Sample Edit / Chop";14->"15 • Save Project";else->"Foundation Challenge"}
 fun explanation()=when(lesson){
  0->"MAIN returns you to the main working view: sequence, track, bank, tempo and transport context."
  1->"BROWSE is where you find and audition sounds, then load the chosen sound onto a pad. WAV and MP3 are supported by this trainer."
  2->"The 16 pads play assigned sounds. Banks A–D give four groups of 16 pad slots."
  3->"REC starts a new recording pass on the current track. A fresh REC pass replaces that track's previous events in this trainer."
  4->"OVERDUB adds new hits while keeping events already recorded on the track."
  5->"Timing Correct aligns recorded hits to a rhythmic grid such as 1/8, 1/16 or 1/32."
  6->"Swing shifts alternating grid positions to move the groove away from straight timing."
  7->"NOTE REPEAT repeatedly triggers a held pad at the selected rhythmic division."
  8->"FULL LEVEL makes pad hits play at maximum velocity. The phone trainer uses fixed velocity rather than pressure-sensitive pads."
  9->"16 LEVELS spreads one selected sound across all 16 pads. This foundation version teaches velocity from low to high."
  10->"STEP SEQ programs rhythm one step at a time."
  11->"TRACK selects a separate part inside the current sequence so layers can stay independent."
  12->"TRACK MUTE silences a track without deleting its events."
  13->"SAMPLE EDIT prepares a WAV or MP3 for basic Start/End trimming and even chopping, then assigns the slices to consecutive pads."
  14->"SAVE stores the trainer project state so the pattern and key settings survive an app restart."
  else->"Use the controls without step-by-step hints and build a basic beat from an empty project."
 }
 fun prompt()=when(lesson){0->"TASK • Press MAIN.";1->"TASK • BROWSE → choose a sound → PREVIEW → LOAD TO PAD.";2->"TASK • Hit a loaded pad.";3->"TASK • REC → play a short pattern → STOP.";4->"TASK • OVERDUB → add another layer → STOP.";5->"TASK • Change the TC division.";6->"TASK • Change Swing.";7->"TASK • Turn NOTE REPEAT on and hold a loaded pad.";8->"TASK • Turn FULL LEVEL on.";9->"TASK • Select a loaded pad and turn 16 LEVELS on.";10->"TASK • Open STEP SEQ and add a step.";11->"TASK • Change TRACK.";12->"TASK • Toggle TRACK MUTE on the selected track.";13->"TASK • Select a loaded musical sample → SAMPLE EDIT → set Start/End → CHOP TO PADS.";14->"TASK • Press SAVE.";else->"CHALLENGE • Build and save a playable beat without guided steps."}
 fun lessonCard()="${title()}\n${explanation()}\n\n${prompt()}"
 private fun hint(action:String)=when(lesson){1->if(action=="browse")"Browser is open. Choose a sound, preview it, then LOAD TO PAD."else prompt();3->if(action=="record")"REC is active. Play at least one loaded pad, then STOP."else prompt();4->if(action=="overdub")"OVERDUB is active. Add at least one new hit, then STOP."else prompt();7->if(action=="note_repeat")"NOTE REPEAT is on. Hold a loaded pad to complete the lesson."else prompt();10->"Opening STEP SEQ is not enough: add a step to the grid.";13->"Chop the selected range and assign the new slices to pads.";else->prompt()}
 companion object{const val LAST_LESSON=15}
}

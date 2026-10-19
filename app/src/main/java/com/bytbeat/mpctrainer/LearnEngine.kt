package com.bytbeat.mpctrainer

/** State-based foundation course driven by real trainer actions. */
class LearnEngine(startLesson:Int=0){
 data class Snapshot(val loadedPads:Int=0,val eventCount:Int=0,val currentTrack:Int=0,val mutedTracks:Int=0,val tc:Int=16,val swing:Int=50,val noteRepeat:Boolean=false,val fullLevel:Boolean=false,val levels16:Boolean=false)
 data class Result(val advanced:Boolean,val lesson:Int,val message:String)
 var lesson:Int=startLesson.coerceIn(0,LAST_LESSON);private set
 private var baselineEvents=0;private var baselineTrack=0;private var baselineSwing=50;private var baselineTc=16;private var baselineMute=0;private var baselinePads=0
 private var challengeRecorded=false;private var challengeOverdub=false;private var challengeTc=false;private var challengeTrack=false;private var challengeStep=false;private var challengeSaved=false
 fun begin(s:Snapshot){baselineEvents=s.eventCount;baselineTrack=s.currentTrack;baselineSwing=s.swing;baselineTc=s.tc;baselineMute=s.mutedTracks;baselinePads=s.loadedPads;if(lesson==LAST_LESSON)resetChallenge()}
 private fun resetChallenge(){challengeRecorded=false;challengeOverdub=false;challengeTc=false;challengeTrack=false;challengeStep=false;challengeSaved=false}
 fun reset(s:Snapshot):Result{lesson=0;resetChallenge();begin(s);return Result(false,lesson,prompt())}
 fun report(action:String,s:Snapshot):Result{
  if(lesson==LAST_LESSON){
   if(action=="stop")challengeRecorded=challengeRecorded||s.eventCount>0
   if(action=="overdub_stop")challengeOverdub=challengeOverdub||s.eventCount>0
   if(action=="tc"&&s.tc!=baselineTc)challengeTc=true
   if(action=="track"&&s.currentTrack!=baselineTrack)challengeTrack=true
   if(action=="step_added")challengeStep=true
   if(action=="save")challengeSaved=true
   val ok=s.loadedPads>=2&&s.eventCount>=2&&challengeRecorded&&challengeOverdub&&challengeTc&&challengeTrack&&challengeStep&&challengeSaved
   return Result(false,lesson,if(ok)"FOUNDATION COMPLETE • You loaded sounds, recorded, overdubbed, corrected timing, worked across tracks, step-sequenced and saved a playable project." else challengeProgress(s))
  }
  val complete=when(lesson){0->action=="main";1->action=="loaded"&&s.loadedPads>baselinePads;2->action=="pad"&&s.loadedPads>0;3->action=="stop"&&s.eventCount>baselineEvents;4->action=="overdub_stop"&&s.eventCount>baselineEvents;5->action=="tc"&&s.tc!=baselineTc;6->action=="swing"&&s.swing!=baselineSwing;7->action=="note_repeat_pad"&&s.noteRepeat;8->action=="full_level_pad"&&s.fullLevel;9->action=="16_levels_pad"&&s.levels16;10->action=="step_added";11->action=="track"&&s.currentTrack!=baselineTrack;12->action=="track_mute"&&s.mutedTracks!=baselineMute;13->action=="chopped"&&s.loadedPads>baselinePads;else->false}
  return if(complete)advance(s)else Result(false,lesson,hint(action))
 }
 private fun advance(s:Snapshot):Result{if(lesson<LAST_LESSON)lesson++;begin(s);return Result(true,lesson,prompt())}
 fun title()=when(lesson){0->"1 • MAIN";1->"2 • Browser";2->"3 • Pads";3->"4 • Record";4->"5 • Overdub";5->"6 • Timing Correct";6->"7 • Swing";7->"8 • Note Repeat";8->"9 • Full Level";9->"10 • 16 Levels";10->"11 • Step Sequencer";11->"12 • Tracks";12->"13 • Track Mute";13->"14 • Sample Edit / Chop";else->"Foundation Challenge"}
 fun explanation()=when(lesson){0->"MAIN returns you to the main working view: sequence, track, bank, tempo and transport context.";1->"BROWSE finds and auditions sounds, then loads the chosen sound onto a pad. WAV and MP3 are supported.";2->"The 16 pads play assigned sounds. Banks A–D provide four groups of 16 pad slots.";3->"REC starts a fresh recording pass on the current track.";4->"OVERDUB adds new hits while keeping events already recorded.";5->"Timing Correct aligns hits to a rhythmic grid such as 1/8, 1/16 or 1/32.";6->"Swing shifts alternating grid positions to change the groove.";7->"NOTE REPEAT repeatedly triggers a held pad at the selected division.";8->"FULL LEVEL makes pad hits play at maximum velocity. Phone pads are not pressure-sensitive.";9->"16 LEVELS spreads one selected sound across all pads; this foundation version teaches velocity.";10->"STEP SEQ programs rhythm one step at a time. This trainer has two bars with 16 steps per bar.";11->"TRACK selects a separate part inside the current sequence.";12->"TRACK MUTE silences a track without deleting events.";13->"SAMPLE EDIT prepares WAV or MP3 for Start/End trimming and even chopping, then assigns slices to consecutive pads.";else->"Build a small project without step-by-step hints. The trainer now checks the core workflow instead of accepting a token beat."}
 fun prompt()=when(lesson){0->"TASK • Press MAIN.";1->"TASK • BROWSE → choose a sound → PREVIEW → LOAD TO PAD.";2->"TASK • Hit a loaded pad.";3->"TASK • REC → play a short pattern → STOP.";4->"TASK • OVERDUB → add another layer → STOP.";5->"TASK • Change TC.";6->"TASK • Change Swing.";7->"TASK • Turn NOTE REPEAT on, then hold a loaded pad.";8->"TASK • Turn FULL LEVEL on and hit a loaded pad.";9->"TASK • Select a loaded pad, turn 16 LEVELS on, then play one of the 16 levels.";10->"TASK • Open STEP SEQ and add a step.";11->"TASK • Change TRACK.";12->"TASK • Toggle TRACK MUTE.";13->"TASK • Select a loaded musical sample → SAMPLE EDIT → Start/End → CHOP TO PADS.";else->"CHALLENGE • Use 2+ loaded pads → REC a pattern → OVERDUB a layer → change TC → use another TRACK → add a Step Seq step → SAVE."}
 fun lessonCard()="${title()}\n${explanation()}\n\n${prompt()}"
 private fun hint(action:String)=when(lesson){1->if(action=="browse")"Choose a sound, preview it, then LOAD TO PAD."else prompt();3->if(action=="record")"Play at least one loaded pad, then STOP."else prompt();4->if(action=="overdub")"Add at least one new hit, then STOP."else prompt();7->if(action=="note_repeat")"NOTE REPEAT is on. Hold a loaded pad so the trainer can verify actual use."else prompt();8->if(action=="full_level")"FULL LEVEL is on. Hit a loaded pad at full velocity."else prompt();9->if(action=="16_levels")"16 LEVELS is on. Play one of the level pads."else prompt();10->"Opening STEP SEQ is not enough: add a step.";13->"Create chops so the trainer can verify new pad assignments.";else->prompt()}
 private fun challengeProgress(s:Snapshot):String{val checks=listOf("2+ pads" to (s.loadedPads>=2),"REC" to challengeRecorded,"2+ events" to (s.eventCount>=2),"OVERDUB" to challengeOverdub,"TC" to challengeTc,"another track" to challengeTrack,"Step Seq" to challengeStep,"SAVE" to challengeSaved);return "CHALLENGE • "+checks.joinToString("  •  "){(n,ok)->"${if(ok)"✓"else"○"} $n"}}
 companion object{const val LAST_LESSON=14}
}

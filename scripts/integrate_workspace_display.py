from pathlib import Path

p=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerActivity.kt')
s=p.read_text()

old='private var metronome=false;private var countIn=true;private var lastBeat=-1L;private var lastClockTick=-1L;private var countInToken=0'
if 'private var workspaceMode=' not in s:
    if old not in s: raise SystemExit('workspace state anchor not found')
    s=s.replace(old,old+'\n private var workspaceMode="MAIN";private var displayOverride:String?=null',1)

old='btn("MAIN"){report("main");refreshStatus()},btn("BROWSE"){report("browse");browse()},btn("SAMPLE EDIT"){sampleEdit()}'
new='btn("MAIN"){setWorkspaceMode("MAIN");report("main")},btn("BROWSE"){setWorkspaceMode("BROWSE");report("browse");browse()},btn("SAMPLE EDIT"){setWorkspaceMode("SAMPLE EDIT");sampleEdit()}'
if new not in s:
    if old not in s: raise SystemExit('top mode buttons anchor not found')
    s=s.replace(old,new,1)
old='btn("STEP SEQ"){steps()}'
new='btn("STEP SEQ"){setWorkspaceMode("STEP SEQ");steps()}'
if new not in s:
    if old not in s: raise SystemExit('step mode anchor not found')
    s=s.replace(old,new,1)

lines=s.splitlines()
idx=next((i for i,line in enumerate(lines) if line.lstrip().startswith('private fun refreshStatus(){')),None)
if idx is None: raise SystemExit('refreshStatus function not found')
if lines[idx].count('{') != lines[idx].count('}'):
    raise SystemExit('refreshStatus is no longer one-line; update integrator deliberately')
block=''' fun setWorkspaceMode(mode:String){workspaceMode=mode;displayOverride=null;refreshStatus()}
 fun showDisplayMessage(message:String){displayOverride=message;refreshStatus()}
 fun clearDisplayMessage(){displayOverride=null;refreshStatus()}
 private fun position():String{val t=tick();val bar=(t/384L)+1;val within=t%384L;val beat=(within/96L)+1;val pulse=within%96L;return "%03d:%02d:%02d".format(bar,beat,pulse)}
 private fun stepPattern(bar:Int):String{
  val start=bar*384L
  return (0 until 16).joinToString(""){step->val t=start+step*24L;if(events.any{it.track==track&&it.bank==bank&&it.pad==pad&&it.tick==t})"■" else "·"}
 }
 private fun workspaceText():String{
  val selected=pads[bank][pad]
  val transport=if(recording)if(overdub)"OVERDUB" else "REC" else if(playing)"PLAY" else "STOP"
  return when(workspaceMode){
   "BROWSE"->"BROWSE  •  BANK ${'A'+bank} / PAD ${pad+1}\\n${selected?.name?:"EMPTY"}\\nВыбери WAV или MP3 • PREVIEW → LOAD TO PAD"
   "SAMPLE EDIT"->"SAMPLE EDIT  •  BANK ${'A'+bank} / PAD ${pad+1}\\n${selected?.name?:"EMPTY"}\\nSTART / END • PREVIEW • CHOP → PADS"
   "STEP SEQ"->"STEP SEQ • TRACK ${track+1} • ${'A'+bank}${pad+1}\\n1 ${stepPattern(0)}\\n2 ${stepPattern(1)}"
   "MIXER"->"MIXER  •  MASTER / PROJECT\\nBANK ${'A'+bank} • PAD ${pad+1} • ${selected?.name?:"EMPTY"}\\nГромкость проекта и выбранного пэда"
   else->"MAIN  •  SEQ 01  •  TRACK ${track+1}${if(mute[track])" MUTE" else ""}\\nBANK ${'A'+bank} • PAD ${pad+1} • ${position()} • $transport\\n$bpm BPM • TC 1/$tc • SWING $swing% • MET ${if(metronome)"ON" else "OFF"} • COUNT ${if(countIn)"ON" else "OFF"}"
  }
 }
 private fun refreshStatus(){TrainerUiState.publish(bank,pad,track,playing,recording,overdub,noteRepeat,fullLevel,levels16,mute[track]);status.text=displayOverride?:workspaceText()}'''
lines[idx:idx+1]=block.splitlines()
s='\n'.join(lines)+'\n'

needle='saveProject();redrawSteps(page,label,bs)}}}'
if needle in s:s=s.replace(needle,'saveProject();redrawSteps(page,label,bs);refreshStatus()}}}',1)
old='AlertDialog.Builder(this).setTitle("STEP SEQUENCER").setView(outer).setPositiveButton("DONE",null).show()'
new='AlertDialog.Builder(this).setTitle("STEP SEQUENCER").setView(outer).setPositiveButton("DONE"){_,_->refreshStatus()}.setOnCancelListener{refreshStatus()}.show()'
if old in s:s=s.replace(old,new,1)
old='status.text="SAMPLE EDIT • ${e.message}";Toast.makeText(this,"Sample Edit: ${e.message}",Toast.LENGTH_LONG).show()'
new='showDisplayMessage("SAMPLE EDIT • ${e.message}");Toast.makeText(this,"SAMPLE EDIT: ${e.message}",Toast.LENGTH_LONG).show()'
if old in s:s=s.replace(old,new,1)
p.write_text(s)

app=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerApp.kt')
a=app.read_text()
old='0->a.showVolumeMixer();1->guide?.performClick()'
new='0->{a.setWorkspaceMode("MIXER");a.showVolumeMixer()};1->guide?.performClick()'
if new not in a:
    if old not in a: raise SystemExit('mixer menu anchor not found')
    a=a.replace(old,new,1)
app.write_text(a)
print('Contextual workspace integrated with stable display override and live Step Seq pattern')
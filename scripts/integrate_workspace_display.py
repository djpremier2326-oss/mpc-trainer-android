from pathlib import Path

p=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerActivity.kt')
s=p.read_text()

old='private var metronome=false;private var countIn=true;private var lastBeat=-1L;private var lastClockTick=-1L;private var countInToken=0'
new=old+'\n private var workspaceMode="MAIN"'
if 'private var workspaceMode=' not in s:
    if old not in s: raise SystemExit('workspace state anchor not found')
    s=s.replace(old,new,1)

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

# Locate refreshStatus structurally. It may sit before any of several helpers after prior integrations.
marker=' private fun refreshStatus(){'
pos=s.find(marker)
if pos<0: raise SystemExit('refreshStatus function not found')
next_positions=[]
for anchor in ('\n private fun loopTicks()', '\n private fun position()', '\n private fun refreshPads()', '\n private fun onDestroy()', '\n private fun name('):
    q=s.find(anchor,pos+len(marker))
    if q>=0: next_positions.append(q)
if not next_positions: raise SystemExit('refreshStatus structural end not found')
end=min(next_positions)
block=''' private fun setWorkspaceMode(mode:String){workspaceMode=mode;refreshStatus()}\n private fun stepPattern(bar:Int):String{\n  val start=bar*384L\n  return (0 until 16).joinToString(""){step->val t=start+step*24L;if(events.any{it.track==track&&it.bank==bank&&it.pad==pad&&it.tick==t})"■" else "·"}\n }\n private fun workspaceText():String{\n  val selected=pads[bank][pad]\n  val transport=if(recording)if(overdub)"OVERDUB" else "REC" else if(playing)"PLAY" else "STOP"\n  return when(workspaceMode){\n   "BROWSE"->"BROWSE  •  BANK ${'A'+bank} / PAD ${pad+1}\\n${selected?.name?:"EMPTY"}\\nВыбери WAV или MP3 • PREVIEW → LOAD TO PAD"\n   "SAMPLE EDIT"->"SAMPLE EDIT  •  BANK ${'A'+bank} / PAD ${pad+1}\\n${selected?.name?:"EMPTY"}\\nSTART / END • PREVIEW • CHOP → PADS"\n   "STEP SEQ"->"STEP SEQ • TRACK ${track+1} • ${'A'+bank}${pad+1}\\n1 ${stepPattern(0)}\\n2 ${stepPattern(1)}"\n   "MIXER"->"MIXER  •  MASTER / PROJECT\\nBANK ${'A'+bank} • PAD ${pad+1} • ${selected?.name?:"EMPTY"}\\nГромкость проекта и выбранного пэда"\n   else->"MAIN  •  SEQ 01  •  TRACK ${track+1}${if(mute[track])" MUTE" else ""}\\nBANK ${'A'+bank} • PAD ${pad+1} • ${position()} • $transport\\n$bpm BPM • TC 1/$tc • SWING $swing% • MET ${if(metronome)"ON" else "OFF"} • COUNT ${if(countIn)"ON" else "OFF"}"\n  }\n }\n private fun refreshStatus(){TrainerUiState.publish(bank,pad,track,playing,recording,overdub,noteRepeat,fullLevel,levels16,mute[track]);status.text=workspaceText()}'''
s=s[:pos]+block+s[end:]

needle='saveProject();redrawSteps(page,label,bs)}}}'
if needle in s:s=s.replace(needle,'saveProject();redrawSteps(page,label,bs);refreshStatus()}}}',1)
old='AlertDialog.Builder(this).setTitle("STEP SEQUENCER").setView(outer).setPositiveButton("DONE",null).show()'
new='AlertDialog.Builder(this).setTitle("STEP SEQUENCER").setView(outer).setPositiveButton("DONE"){_,_->refreshStatus()}.setOnCancelListener{refreshStatus()}.show()'
if old in s:s=s.replace(old,new,1)
old='status.text="SAMPLE EDIT • ${e.message}";Toast.makeText(this,"Sample Edit: ${e.message}",Toast.LENGTH_LONG).show()'
new='status.text="SAMPLE EDIT • ${e.message}";Toast.makeText(this,"SAMPLE EDIT: ${e.message}",Toast.LENGTH_LONG).show();h.postDelayed({refreshStatus()},1800)'
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
print('Contextual workspace integrated with live Step Seq pattern')
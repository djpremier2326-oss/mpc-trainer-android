from pathlib import Path
p=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerActivity.kt')
s=p.read_text()
# Add persistent waveform overlay state.
anchor='private lateinit var status:TextView; private lateinit var learnCard:TextView; private lateinit var engine:LearnEngine'
repl=anchor+'\n private var displayWaveform:WaveformView?=null;private var displayWaveFile:File?=null'
if 'displayWaveform' not in s:
    if anchor not in s: raise SystemExit('status anchor missing')
    s=s.replace(anchor,repl,1)
# Replace the status TextView in its parent with a FrameLayout containing text + waveform.
needle='root.addView(status,LinearLayout.LayoutParams(-1,72));learnCard='
replacement='''val displayFrame=FrameLayout(this);displayFrame.addView(status,FrameLayout.LayoutParams(-1,-1));displayWaveform=WaveformView(this).apply{visibility=View.GONE};displayFrame.addView(displayWaveform,FrameLayout.LayoutParams(-1,-1));root.addView(displayFrame,LinearLayout.LayoutParams(-1,190));learnCard='''
if needle not in s: raise SystemExit('display layout anchor missing')
s=s.replace(needle,replacement,1)
# Once Sample Edit has normalized/inspected audio, put the real waveform on the main display before opening controls.
needle='WavSlicer.inspect(editable);runOnUiThread{showSampleEditor(editable,s.name)}'
replacement='WavSlicer.inspect(editable);runOnUiThread{displayWaveFile=editable;displayWaveform?.loadPcm16Wav(editable);displayWaveform?.visibility=View.VISIBLE;status.visibility=View.GONE;showSampleEditor(editable,s.name)}'
if needle not in s: raise SystemExit('sample prepare anchor missing')
s=s.replace(needle,replacement,1)
# Other workspace modes restore textual display; SAMPLE EDIT keeps waveform when available.
needle='fun setWorkspaceMode(mode:String){workspaceMode=mode;refreshStatus()}'
replacement='fun setWorkspaceMode(mode:String){workspaceMode=mode;if(mode=="SAMPLE EDIT"&&displayWaveFile!=null){status.visibility=View.GONE;displayWaveform?.visibility=View.VISIBLE}else{displayWaveform?.visibility=View.GONE;status.visibility=View.VISIBLE};refreshStatus()}'
if needle not in s: raise SystemExit('workspace mode anchor missing; run after workspace integration')
s=s.replace(needle,replacement,1)
# Keep main waveform selection synchronized with Sample Edit controls.
needle='if(!fromWave)waveform.setSelection(start.progress/100f,end.progress/100f);read.text='
replacement='if(!fromWave){waveform.setSelection(start.progress/100f,end.progress/100f);displayWaveform?.setSelection(start.progress/100f,end.progress/100f)};read.text='
if needle not in s: raise SystemExit('selection anchor missing; run after waveform integration')
s=s.replace(needle,replacement,1)
p.write_text(s)
print('Main MPC display now shows the real Sample Edit waveform')
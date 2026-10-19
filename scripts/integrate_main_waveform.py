from pathlib import Path
p=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerActivity.kt')
s=p.read_text()
anchor='private lateinit var status:TextView; private lateinit var learnCard:TextView; private lateinit var engine:LearnEngine'
repl=anchor+'\n private var displayWaveform:WaveformView?=null;private var displayInfo:TextView?=null'
if 'displayWaveform' not in s:
    if anchor not in s: raise SystemExit('status anchor missing')
    s=s.replace(anchor,repl,1)
needle='root.addView(status,LinearLayout.LayoutParams(-1,72));learnCard='
replacement='''val displayFrame=FrameLayout(this);displayFrame.addView(status,FrameLayout.LayoutParams(-1,-1));displayWaveform=WaveformView(this).apply{visibility=View.GONE};displayFrame.addView(displayWaveform,FrameLayout.LayoutParams(-1,-1));displayInfo=TextView(this).apply{setTextColor(Color.rgb(235,238,222));textSize=10f;visibility=View.GONE;gravity=Gravity.BOTTOM or Gravity.START;setPadding(10,0,10,6)};displayFrame.addView(displayInfo,FrameLayout.LayoutParams(-1,-1));root.addView(displayFrame,LinearLayout.LayoutParams(-1,190));learnCard='''
if needle not in s: raise SystemExit('display layout anchor missing')
s=s.replace(needle,replacement,1)
# Expose selected-pad volume to the phone surface. The step mutator is owned by integrate_volume_controls.py.
needle='private fun sampleEdit(){'
helper='''fun selectedPadVolumePercent():Int=(padVolumes[bank][pad]*100f).toInt().coerceIn(0,100)
 fun setSelectedPadVolumePercent(value:Int){padVolumes[bank][pad]=(value.coerceIn(0,100)/100f);saveVolumes();report("mixer_changed");refreshStatus()}
 fun selectedPadLabel():String="${'A'+bank}${pad+1}"
 private fun sampleEdit(){'''
if needle not in s: raise SystemExit('sampleEdit anchor missing')
s=s.replace(needle,helper,1)
needle='WavSlicer.inspect(editable);runOnUiThread{showSampleEditor(editable,s.name)}'
replacement='WavSlicer.inspect(editable);runOnUiThread{displayWaveform?.loadPcm16Wav(editable);displayWaveform?.setSelection(0f,1f);displayWaveform?.visibility=View.VISIBLE;displayInfo?.visibility=View.VISIBLE;displayInfo?.text="SAMPLE EDIT   BANK ${\'A\'+bank} / PAD ${pad+1}   ${s.name}";status.visibility=View.GONE;showSampleEditor(editable,s.name)}'
if needle not in s: raise SystemExit('sample prepare anchor missing')
s=s.replace(needle,replacement,1)
needle='fun setWorkspaceMode(mode:String){workspaceMode=mode;refreshStatus()}'
replacement='fun setWorkspaceMode(mode:String){workspaceMode=mode;if(mode!="SAMPLE EDIT"){displayWaveform?.visibility=View.GONE;displayInfo?.visibility=View.GONE;status.visibility=View.VISIBLE};refreshStatus()}'
if needle not in s: raise SystemExit('workspace mode anchor missing')
s=s.replace(needle,replacement,1)
needle='if(!fromWave)waveform.setSelection(start.progress/100f,end.progress/100f);read.text='
replacement='if(!fromWave){waveform.setSelection(start.progress/100f,end.progress/100f);displayWaveform?.setSelection(start.progress/100f,end.progress/100f)};read.text='
if needle not in s: raise SystemExit('selection anchor missing')
s=s.replace(needle,replacement,1)
p.write_text(s)
print('Stable main display; waveform only in Sample Edit; pad volume exposed for hardware-style control')
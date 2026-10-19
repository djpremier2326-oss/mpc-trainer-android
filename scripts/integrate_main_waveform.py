from pathlib import Path
p=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerActivity.kt')
s=p.read_text()
anchor='private lateinit var status:TextView; private lateinit var learnCard:TextView; private lateinit var engine:LearnEngine'
repl=anchor+'\n private var displayWaveform:WaveformView?=null;private var displayWaveFile:File?=null;private var displayInfo:TextView?=null;private var displayPadVolume:SeekBar?=null'
if 'displayWaveform' not in s:
    if anchor not in s: raise SystemExit('status anchor missing')
    s=s.replace(anchor,repl,1)
needle='root.addView(status,LinearLayout.LayoutParams(-1,72));learnCard='
replacement='''val displayFrame=FrameLayout(this);displayFrame.addView(status,FrameLayout.LayoutParams(-1,-1));displayWaveform=WaveformView(this).apply{visibility=View.GONE};displayFrame.addView(displayWaveform,FrameLayout.LayoutParams(-1,-1));val overlay=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.BOTTOM;setPadding(10,0,10,4)};displayInfo=TextView(this).apply{setTextColor(Color.rgb(235,238,222));textSize=10f;visibility=View.GONE};displayPadVolume=SeekBar(this).apply{max=100;visibility=View.GONE;setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(v:SeekBar?,x:Int,from:Boolean){if(from){padVolumes[bank][pad]=(x/100f).coerceIn(0f,1f);saveVolumes();displayInfo?.text="BANK ${'A'+bank}  PAD ${pad+1}   VOL $x%   ${pads[bank][pad]?.name?:"EMPTY"}"}};override fun onStartTrackingTouch(v:SeekBar?){};override fun onStopTrackingTouch(v:SeekBar?){report("mixer_changed")}})};overlay.addView(displayInfo,LinearLayout.LayoutParams(-1,26));overlay.addView(displayPadVolume,LinearLayout.LayoutParams(-1,32));displayFrame.addView(overlay,FrameLayout.LayoutParams(-1,-1));root.addView(displayFrame,LinearLayout.LayoutParams(-1,190));learnCard='''
if needle not in s: raise SystemExit('display layout anchor missing')
s=s.replace(needle,replacement,1)
# Selecting/hitting a pad refreshes the display after the normal pad action.
needle='pad=i;refreshPads();hitPad(i)'
replacement='pad=i;refreshPads();hitPad(i);showSelectedPadOnDisplay()'
if needle not in s: raise SystemExit('pad click anchor missing')
s=s.replace(needle,replacement,1)
# Add selected-pad display helper before Sample Edit.
needle='private fun sampleEdit(){'
helper='''private fun showSelectedPadOnDisplay(){val smp=pads[bank][pad];if(smp==null){displayWaveFile=null;displayWaveform?.visibility=View.GONE;displayInfo?.visibility=View.VISIBLE;displayPadVolume?.visibility=View.GONE;status.visibility=View.GONE;displayInfo?.text="BANK ${'A'+bank}  PAD ${pad+1}   EMPTY";return};displayInfo?.visibility=View.VISIBLE;displayPadVolume?.visibility=View.VISIBLE;displayPadVolume?.progress=(padVolumes[bank][pad]*100).toInt();displayInfo?.text="BANK ${'A'+bank}  PAD ${pad+1}   VOL ${(padVolumes[bank][pad]*100).toInt()}%   ${smp.name}";status.visibility=View.GONE;thread{name="display-wave";try{val src=File(smp.path);val editable=AudioImport.toEditableWav(src,File(cacheDir,"display_${bank}_${pad}.wav"));WavSlicer.inspect(editable);runOnUiThread{if(pads[bank][pad]?.path==smp.path){displayWaveFile=editable;displayWaveform?.loadPcm16Wav(editable);displayWaveform?.setSelection(0f,1f);displayWaveform?.visibility=View.VISIBLE}}}catch(_:Exception){runOnUiThread{displayWaveform?.visibility=View.GONE}}}.start()}
 private fun sampleEdit(){'''
if needle not in s: raise SystemExit('sampleEdit anchor missing')
s=s.replace(needle,helper,1)
needle='WavSlicer.inspect(editable);runOnUiThread{showSampleEditor(editable,s.name)}'
replacement='WavSlicer.inspect(editable);runOnUiThread{displayWaveFile=editable;displayWaveform?.loadPcm16Wav(editable);displayWaveform?.visibility=View.VISIBLE;displayInfo?.visibility=View.VISIBLE;displayPadVolume?.visibility=View.VISIBLE;status.visibility=View.GONE;showSampleEditor(editable,s.name)}'
if needle not in s: raise SystemExit('sample prepare anchor missing')
s=s.replace(needle,replacement,1)
needle='fun setWorkspaceMode(mode:String){workspaceMode=mode;refreshStatus()}'
replacement='fun setWorkspaceMode(mode:String){workspaceMode=mode;if((mode=="MAIN"||mode=="SAMPLE EDIT")&&pads[bank][pad]!=null){showSelectedPadOnDisplay()}else{displayWaveform?.visibility=View.GONE;displayInfo?.visibility=View.GONE;displayPadVolume?.visibility=View.GONE;status.visibility=View.VISIBLE};refreshStatus()}'
if needle not in s: raise SystemExit('workspace mode anchor missing')
s=s.replace(needle,replacement,1)
needle='if(!fromWave)waveform.setSelection(start.progress/100f,end.progress/100f);read.text='
replacement='if(!fromWave){waveform.setSelection(start.progress/100f,end.progress/100f);displayWaveform?.setSelection(start.progress/100f,end.progress/100f)};read.text='
if needle not in s: raise SystemExit('selection anchor missing')
s=s.replace(needle,replacement,1)
p.write_text(s)
print('Loaded pad now shows waveform + live pad volume in the main display')
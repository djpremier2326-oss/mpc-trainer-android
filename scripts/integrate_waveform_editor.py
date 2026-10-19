from pathlib import Path
p=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerActivity.kt')
s=p.read_text()
old='val read=TextView(this);val start=SeekBar(this).apply{max=95;progress=0};val end=SeekBar(this).apply{max=100;progress=100};val pieces=SeekBar(this).apply{max=14;progress=6};fun update(){if(end.progress<=start.progress)end.progress=(start.progress+1).coerceAtMost(100);read.text="START ${start.progress}%   END ${end.progress}%\\nCHOPS ${pieces.progress+2} • first slice → PAD ${pad+1}"}'
new='val read=TextView(this);val waveform=WaveformView(this).apply{minimumHeight=(150*resources.displayMetrics.density).toInt();loadPcm16Wav(src)};val start=SeekBar(this).apply{max=95;progress=0};val end=SeekBar(this).apply{max=100;progress=100};val pieces=SeekBar(this).apply{max=14;progress=6};var fromWave=false;fun update(){if(end.progress<=start.progress)end.progress=(start.progress+1).coerceAtMost(100);if(!fromWave)waveform.setSelection(start.progress/100f,end.progress/100f);read.text="START ${start.progress}%   END ${end.progress}%\\nCHOPS ${pieces.progress+2} • первый слайс → PAD ${pad+1}"};waveform.onSelectionChanged={a,b->fromWave=true;start.progress=(a*100).toInt().coerceIn(0,95);end.progress=(b*100).toInt().coerceIn(start.progress+1,100);fromWave=false;update()}'
if old not in s: raise SystemExit('sample editor state anchor not found')
s=s.replace(old,new,1)
old='box.addView(TextView(this).apply{text="$label\\nSet Start/End, then chop the selected range evenly across pads."});box.addView(read);'
new='box.addView(TextView(this).apply{text="$label\\nДвигай жёлтые границы прямо на волне или используй Start/End. CHOP разложит выбранный участок по пэдам."});box.addView(waveform,LinearLayout.LayoutParams(-1,(170*resources.displayMetrics.density).toInt()));box.addView(read);'
if old not in s: raise SystemExit('sample editor content anchor not found')
s=s.replace(old,new,1)
s=s.replace('text="NUMBER OF CHOPS"','text="КОЛИЧЕСТВО CHOPS"',1)
p.write_text(s)
print('Interactive native waveform integrated into SAMPLE EDIT')
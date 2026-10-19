from pathlib import Path
p=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerActivity.kt')
s=p.read_text()
# After volume integration the manual path is volume-aware. Normalize it to the
# exact anchor expected by integrate_pitch_quantize; that script then adds rate.
candidates=[
 'val g=(padGain(v)*padVolumes[b][p]*masterVolume).coerceIn(0f,1f);pads[b][p]?.let{playLoadedSound(it.soundId,g,true)}',
 'pads[b][p]?.let{val g=(padGain(v)*padVolumes[b][p]*masterVolume).coerceIn(0f,1f);playLoadedSound(it.soundId,g,true)}',
 'pads[b][p]?.let{val g=(padGain(v)*padVolumes[b][p]*masterVolume).coerceIn(0f,1f);pool.play(it.soundId,g,g,1,0,1f)}'
]
target='val g=padGain(v,b,p);pads[b][p]?.let{playLoadedSound(it.soundId,g,true,pitchRate(b,p))}'
expected='val g=padGain(v,b,p);pads[b][p]?.let{playLoadedSound(it.soundId,g,true,pitchRate(b,p))}'
# Do not add pitchRate here before the pitch script defines it. We only need to
# convert direct pool.play to playLoadedSound while preserving volume gain.
bridge='val g=(padGain(v)*padVolumes[b][p]*masterVolume).coerceIn(0f,1f);pads[b][p]?.let{playLoadedSound(it.soundId,g,true)}'
if bridge in s:
 print('Manual playback already uses playLoadedSound; pitch script must accept volume-aware anchor')
elif candidates[1] in s:
 s=s.replace(candidates[1],bridge,1);p.write_text(s);print('Manual playback normalized for pitch integration')
elif candidates[2] in s:
 s=s.replace(candidates[2],bridge,1);p.write_text(s);print('Manual playback bridged from direct SoundPool')
elif target in s or expected in s:
 print('Pitch anchor already bridged')
else:
 raise SystemExit('manual playback bridge anchor missing')

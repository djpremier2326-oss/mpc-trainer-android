from pathlib import Path

p=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerActivity.kt')
s=p.read_text()

# Runtime-only identity map: a just-recorded event is monitored once from the pad, then becomes eligible for sequencer playback on the next loop.
old='private val events=mutableListOf<Event>()'
new='private val events=mutableListOf<Event>()\n private val recordedCycle=java.util.IdentityHashMap<Event,Long>()'
if old not in s: raise SystemExit('events anchor not found')
s=s.replace(old,new,1)

old='private fun tick()=if(!playing)0L else (((SystemClock.elapsedRealtime()-startMs)/(60000.0/bpm/96.0)).toLong()%loopTicks())'
new='private fun absoluteTick()=if(!playing)0L else ((SystemClock.elapsedRealtime()-startMs)/(60000.0/bpm/96.0)).toLong()\n private fun tick()=absoluteTick()%loopTicks()\n private fun loopCycle()=absoluteTick()/loopTicks()'
if old not in s: raise SystemExit('tick anchor not found')
s=s.replace(old,new,1)

old='events+=Event(b,p,quant(tick()),track,v);saveProject()'
new='val e=Event(b,p,quant(tick()),track,v);events+=e;recordedCycle[e]=loopCycle();saveProject()'
if old not in s: raise SystemExit('record event anchor not found')
s=s.replace(old,new,1)

# Apply to clock after volume integration has rewritten playback.
old='events.filter{!mute[it.track]&&crossed(lastClockTick,t,it.tick)}.forEach{e->pads[e.bank][e.pad]?.let{val base=if(e.velocity>=100)1f else e.velocity.coerceIn(1,99)/100f;val g=(base*padVolumes[e.bank][e.pad]*masterVolume).coerceIn(0f,1f);pool.play(it.soundId,g,g,1,0,1f)}}'
new='val cycle=loopCycle();events.filter{!mute[it.track]&&crossed(lastClockTick,t,it.tick)&&(recordedCycle[it]?.let{born->cycle>born}?:true)}.forEach{e->pads[e.bank][e.pad]?.let{val base=if(e.velocity>=100)1f else e.velocity.coerceIn(1,99)/100f;val g=(base*padVolumes[e.bank][e.pad]*masterVolume).coerceIn(0f,1f);pool.play(it.soundId,g,g,1,0,1f)}}'
if old not in s: raise SystemExit('clock playback anchor not found')
s=s.replace(old,new,1)

# New non-overdub recording discards a track, so discard stale runtime suppression entries too.
old='if(!od)events.removeAll{it.track==track};overdub=od'
new='if(!od){events.removeAll{it.track==track};recordedCycle.keys.removeAll{it.track==track}};overdub=od'
if old not in s: raise SystemExit('record clear anchor not found')
s=s.replace(old,new,1)

p.write_text(s)
print('Live record monitoring fixed: pad hit sounds once; captured event starts sequencer playback next loop')
from pathlib import Path

p=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerActivity.kt')
s=p.read_text()

old='private val events=mutableListOf<Event>()'
new='private val events=mutableListOf<Event>()\n private val recordedCycle=java.util.IdentityHashMap<Event,Long>()'
if new not in s:
    if old not in s: raise SystemExit('events anchor not found')
    s=s.replace(old,new,1)

old='private fun tick()=if(!playing)0L else (((SystemClock.elapsedRealtime()-startMs)/(60000.0/bpm/96.0)).toLong()%loopTicks())'
new='private fun absoluteTick()=if(!playing)0L else ((SystemClock.elapsedRealtime()-startMs)/(60000.0/bpm/96.0)).toLong()\n private fun tick()=absoluteTick()%loopTicks()\n private fun loopCycle()=absoluteTick()/loopTicks()'
if new not in s:
    if old not in s: raise SystemExit('tick anchor not found')
    s=s.replace(old,new,1)

old='events+=Event(b,p,quant(tick()),track,v);saveProject()'
new='val e=Event(b,p,quant(tick()),track,v);events+=e;recordedCycle[e]=loopCycle();saveProject()'
if new not in s:
    if old not in s: raise SystemExit('record event anchor not found')
    s=s.replace(old,new,1)

# The original clock uses a single-expression if. Wrap the filtered playback in braces
# before declaring cycle, otherwise Kotlin sees `if(condition) val ...`.
needle='if(lastClockTick>=0&&t!=lastClockTick)events.filter{!mute[it.track]&&crossed(lastClockTick,t,it.tick)}.forEach{e->'
replacement='if(lastClockTick>=0&&t!=lastClockTick){val cycle=loopCycle();events.filter{ev->!mute[ev.track]&&crossed(lastClockTick,t,ev.tick)&&(recordedCycle[ev]?.let{born->cycle>born}?:true)}.forEach{e->'
if replacement not in s:
    if needle not in s: raise SystemExit('clock event-filter anchor not found')
    s=s.replace(needle,replacement,1)
    # close the new if block after the existing forEach playback expression
    playback_end='pool.play(it.soundId,g,g,1,0,1f)}};lastClockTick=t'
    playback_closed='pool.play(it.soundId,g,g,1,0,1f)}}};lastClockTick=t'
    if playback_end not in s: raise SystemExit('clock playback closing anchor not found')
    s=s.replace(playback_end,playback_closed,1)

old='if(!od)events.removeAll{it.track==track};overdub=od'
new='if(!od){events.removeAll{it.track==track};recordedCycle.keys.removeAll{it.track==track}};overdub=od'
if new not in s:
    if old not in s: raise SystemExit('record clear anchor not found')
    s=s.replace(old,new,1)

p.write_text(s)
print('Live record monitoring fixed: newly captured hits are monitored once and join sequencer playback next loop')
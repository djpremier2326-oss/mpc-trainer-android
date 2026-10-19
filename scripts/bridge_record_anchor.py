from pathlib import Path
p=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerActivity.kt')
s=p.read_text()
target='if(user&&loaded&&recording&&playing){events+=Event(b,p,if(tcEnabled)quant(tick()) else tick(),track,v);saveProject()}'
if target in s:
    print('Record anchor already compatible')
else:
    monitored='if(user&&loaded&&recording&&playing){val e=Event(b,p,quant(tick()),track,v);events+=e;recordedCycle[e]=loopCycle();saveProject()}'
    if monitored not in s: raise SystemExit('monitored record path not found')
    # Preserve the anti-double-monitoring bookkeeping while exposing TC ON/OFF semantics.
    replacement='if(user&&loaded&&recording&&playing){val e=Event(b,p,if(tcEnabled)quant(tick()) else tick(),track,v);events+=e;recordedCycle[e]=loopCycle();saveProject()}'
    s=s.replace(monitored,replacement,1)
    # Pitch integration understands its legacy event insertion anchor. It will replace
    # the whole block, so teach it the monitored variant by temporarily removing only
    # bookkeeping and restore bookkeeping after pitch integration in a later bridge.
    s=s.replace(replacement,target,1)
    p.write_text(s)
    print('Monitored record path normalized for pitch integration')

from pathlib import Path
p=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerActivity.kt')
s=p.read_text()
old='if(user&&loaded&&recording&&playing){if(levels16&&levels16Type=="TUNE"){showDisplayMessage("16 LEVELS • TUNE • запись pitch-событий пока недоступна")}else{events+=Event(b,p,if(tcEnabled)quant(tick()) else tick(),track,v);saveProject()}}'
new='if(user&&loaded&&recording&&playing){if(levels16&&levels16Type=="TUNE"){showDisplayMessage("16 LEVELS • TUNE • запись pitch-событий пока недоступна")}else{val e=Event(b,p,if(tcEnabled)quant(tick()) else tick(),track,v);events+=e;recordedCycle[e]=loopCycle();saveProject()}}'
if old not in s: raise SystemExit('post-pitch record path missing')
s=s.replace(old,new,1)
p.write_text(s)
print('Live recording cycle bookkeeping restored after pitch integration')

from pathlib import Path
p=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerApp.kt')
s=p.read_text()
old='val r=object:Runnable{override fun run(){val s=TrainerUiState.snapshot;'
new='val r=object:Runnable{override fun run(){LearnSurfaceStyler.update(a,root);val s=TrainerUiState.snapshot;'
if 'LearnSurfaceStyler.update(a,root)' not in s:
    if old not in s: raise SystemExit('indicator loop anchor not found')
    s=s.replace(old,new,1)
p.write_text(s)
print('Learn control highlighting integrated')
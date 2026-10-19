from pathlib import Path
import re
p=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerActivity.kt')
s=p.read_text()
# Normalize the transformed Step Sequencer method back to the semantic form
# expected by integrate_pitch_quantize.py. Phone sizing changes are preserved
# elsewhere in steps(); only redrawSteps is normalized here.
target='private fun redrawSteps(page:Int,label:TextView,bs:List<Button>){label.text="BAR ${page+1} • TRACK ${track+1} • PAD ${pad+1}";bs.forEachIndexed{i,b->val t=page*384L+i*24L;val on=events.any{it.track==track&&it.bank==bank&&it.pad==pad&&it.tick==t};b.text="${i+1}\\n${if(on)"●" else "○"}";b.setOnClickListener{val ix=events.indexOfFirst{it.track==track&&it.bank==bank&&it.pad==pad&&it.tick==t};if(ix>=0)events.removeAt(ix)else{events+=Event(bank,pad,t,track,127);report("step_added")};saveProject();redrawSteps(page,label,bs)}}}'
if target in s:
    print('Step redraw already normalized for pitch integration')
else:
    start=s.find('private fun redrawSteps(')
    end=s.find('\n private fun browse()',start)
    if start<0 or end<0: raise SystemExit('step redraw method bounds missing')
    current=s[start:end]
    # Only accept the known Step Sequencer shape; do not hide unrelated source changes.
    required=['page*384L+i*24L','events','step_added','redrawSteps(page,label,bs)']
    if not all(x in current for x in required):
        raise SystemExit('step redraw semantic guard failed')
    s=s[:start]+target+s[end:]
    p.write_text(s)
    print('Step redraw normalized for pitch integration')

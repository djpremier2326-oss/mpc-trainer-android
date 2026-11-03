from pathlib import Path

path = Path('app/src/main/assets/index.html')
text = path.read_text(encoding='utf-8')
old = "function violetOpenEditor(index=null){const rows=ensureHourlyDay(selected),panel=$('violet-task-editor');if(!panel)return;violetEditorIndex=index;const edit=index!==null&&index>=0,row=edit?rows[index]:null;const now=new Date(),defaultStart=String(now.getHours()).padStart(2,'0')+':'+String(Math.floor(now.getMinutes()/5)*5).padStart(2,'0');$('violet-editor-title').textContent=edit?'Изменить задание':'Новое задание';$('violet-editor-text').value=row?.text||'';$('violet-editor-start').value=row?.time||defaultStart;$('violet-editor-end').value=row?violetTaskEnd(row):violetDefaultEnd(defaultStart);$('violet-editor-error').hidden=true;panel.hidden=false;document.body.classList.add('violet-editor-open');setTimeout(()=>$('violet-editor-text')?.focus(),40)}"
new = "function violetOpenEditor(index=null){const rows=ensureHourlyDay(selected),panel=$('violet-task-editor');if(!panel)return;violetEditorIndex=index;const edit=index!==null&&index>=0,row=edit?rows[index]:null;const defaultStart='00:00',defaultEnd='00:00';$('violet-editor-title').textContent=edit?'Изменить задание':'Новое задание';$('violet-editor-text').value=row?.text||'';$('violet-editor-start').value=row?.time||defaultStart;$('violet-editor-end').value=row?violetTaskEnd(row):defaultEnd;$('violet-editor-error').hidden=true;panel.hidden=false;document.body.classList.add('violet-editor-open');setTimeout(()=>$('violet-editor-text')?.focus(),40)}"
if old not in text:
    raise SystemExit('Violet zero-time patch failed: editor function not found')
text = text.replace(old, new, 1)
path.write_text(text, encoding='utf-8')
print('Violet new-task editor defaults both time fields to 00:00')

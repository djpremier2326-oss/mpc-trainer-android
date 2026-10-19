from pathlib import Path

p=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerActivity.kt')
s=p.read_text()

old='private fun report(a:String){if(!learn)return;val r=engine.report(a,snapshot());if(r.advanced){saveLesson();Toast.makeText(this,"Lesson complete",Toast.LENGTH_SHORT).show()};learnCard.text=if(r.advanced)engine.lessonCard() else "${engine.title()}\\n${engine.explanation()}\\n\\n${r.message}"}'
new='private fun report(a:String){if(!learn)return;val r=engine.report(a,snapshot());if(r.advanced){saveLesson();Toast.makeText(this,"Lesson complete",Toast.LENGTH_SHORT).show()};learnCard.text="${engine.title()}\\n${if(r.advanced)engine.prompt() else r.message}"}'
if old not in s: raise SystemExit('report anchor not found')
s=s.replace(old,new,1)

old='private fun showLesson(){learnCard.text=engine.lessonCard()}'
new='private fun showLesson(){learnCard.text="${engine.title()}\\n${engine.prompt()}"}'
if old not in s: raise SystemExit('showLesson anchor not found')
s=s.replace(old,new,1)

p.write_text(s)
print('Compact action-first Learn surface integrated')
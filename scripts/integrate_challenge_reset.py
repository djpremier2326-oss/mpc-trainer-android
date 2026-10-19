from pathlib import Path

app=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerApp.kt')
s=app.read_text()
old='ProjectReset.preferenceKeys().forEach(e::remove);e.apply();a.recreate()'
new='ProjectReset.preferenceKeys().forEach(e::remove);e.apply();a.getSharedPreferences("learn",Context.MODE_PRIVATE).edit().putBoolean("new_project_pending",true).apply();a.recreate()'
if new not in s:
    if old not in s: raise SystemExit('TrainerApp new-project anchor not found')
    s=s.replace(old,new,1)
app.write_text(s)

activity=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerActivity.kt')
s=activity.read_text()
anchor='engine.begin(snapshot());showLesson();refreshStatus()'
insert='engine.begin(snapshot());val resetPrefs=getSharedPreferences("learn",MODE_PRIVATE);if(resetPrefs.getBoolean("new_project_pending",false)){resetPrefs.edit().remove("new_project_pending").apply();if(engine.lesson==LearnEngine.LAST_LESSON){learn=true;learnCard.visibility=View.VISIBLE;engine.report("new_project",snapshot());saveLesson()}};showLesson();refreshStatus()'
if insert not in s:
    if anchor not in s: raise SystemExit('TrainerActivity learn startup anchor not found')
    s=s.replace(anchor,insert,1)
activity.write_text(s)
print('New Project now resumes and arms the clean foundation challenge')
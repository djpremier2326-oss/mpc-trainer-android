from pathlib import Path
p=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerActivity.kt')
s=p.read_text()
def replace(old,new):
 global s
 if s.count(old)!=1:raise SystemExit('Teaching anchor not unique: '+old[:100])
 s=s.replace(old,new,1)
replace('private var workspaceMode="MAIN";', 'private var lessonReady=false;private var lessonDialog:AlertDialog?=null;private var recordArmed=false;private var pausedTick=0L;private var passRecorded=false;private var passOverdub=false\n private var workspaceMode="MAIN";')
# Opening Learn or a lesson shows explanatory text first. The compact card remains
# tappable, so explanations never compete with the full pad surface for space.
old=next(line for line in s.splitlines() if line.startswith(' private fun report(a:String)'))
replace(old,''' private fun report(a:String){if(!learn)return;if(!lessonReady){renderLessonCard();return};val r=engine.report(a,snapshot());if(r.advanced){saveLesson();lessonReady=false;stopRepeat();Toast.makeText(this,"Практика выполнена. Разберём следующий урок",Toast.LENGTH_SHORT).show();renderLessonCard();if(!playing)h.post{showLessonExplanation()}}else{learnCard.text="${engine.title()}\\n${r.message}\\nНажми карточку: объяснение и пример"}}''')
old='private fun showLesson(){learnCard.text="${engine.title()}\\n${engine.prompt()}"}'
replace(old,'private fun showLesson(){renderLessonCard();showLessonExplanation()}')
replace('root.addView(learnCard)','learnCard.setOnClickListener{showLessonExplanation()};root.addView(learnCard)')
block=r'''
 private fun renderLessonCard(){learnCard.text="${engine.title()}\n${if(lessonReady)engine.prompt() else "ЗАДАНИЕ • Сначала разбери урок: нажми эту карточку"}\nНажми: объяснение • звуковой пример"}
 private fun showLessonExplanation(){
  if(isFinishing||isDestroyed)return
  lessonDialog?.dismiss()
  val content=LessonContent.get(engine.lesson)
  val column=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(densityDp(20),densityDp(8),densityDp(20),densityDp(12))}
  fun section(title:String,body:String){column.addView(TextView(this).apply{text=title;textSize=17f;setTypeface(typeface,1);setPadding(0,densityDp(14),0,densityDp(5))});column.addView(TextView(this).apply{text=body;textSize=16f;setLineSpacing(densityDp(3).toFloat(),1f)})}
  section("Что это",content.meaning);section("Зачем это нужно",content.purpose);section("Что услышать в примере",content.listen)
  column.addView(Button(this).apply{text="▶ Послушать пример · 90 BPM";setOnClickListener{playTeachingExample(engine.lesson)}})
  section("Практика",content.practice)
  val scroll=ScrollView(this).apply{addView(column)}
  lessonDialog=AlertDialog.Builder(this).setTitle("${engine.lesson+1}/${LessonContent.lessons.size} · ${content.title}").setView(scroll).setPositiveButton(if(lessonReady)"Вернуться к практике" else "Проверить понимание"){_,_->browserPreview.stop();if(lessonReady)renderLessonCard()else showUnderstandingCheck()}.setNeutralButton("Все уроки / звуки"){_,_->browserPreview.stop();showCourseMenu()}.setNegativeButton("Закрыть"){_,_->browserPreview.stop()}.create()
  lessonDialog?.setOnDismissListener{browserPreview.stop()};lessonDialog?.show()
 }
 private fun showUnderstandingCheck(){
  val lesson=engine.lesson;val content=LessonContent.get(lesson)
  AlertDialog.Builder(this).setTitle(content.check).setItems(content.answers.toTypedArray()){_,answer->
   val right=answer==content.correct
   AlertDialog.Builder(this).setTitle(if(right)"Верно" else "Разберёмся").setMessage(content.feedback).setPositiveButton(if(right)"К практике" else "Попробовать ещё"){_,_->if(right&&engine.lesson==lesson){lessonReady=true;renderLessonCard()}else showUnderstandingCheck()}.setNegativeButton("Объяснение"){_,_->showLessonExplanation()}.show()
  }.setNegativeButton("Назад"){_,_->showLessonExplanation()}.show()
 }
 private fun showCourseMenu(){
  val items=(listOf("Добавить учебные звуки в свободные пэды")+LessonContent.lessons.mapIndexed{i,l->"${i+1}. ${l.title}"}).toTypedArray()
  AlertDialog.Builder(this).setTitle("Курс MPC").setItems(items){_,i->if(i==0){addTeachingKit();showLessonExplanation()}else{if(playing||recordArmed){Toast.makeText(this,"Сначала останови транспорт",Toast.LENGTH_SHORT).show()}else{engine.selectLesson(i-1,snapshot());saveLesson();lessonReady=false;learn=true;learnCard.visibility=View.VISIBLE;showLesson()}}}.setNegativeButton("Закрыть",null).show()
 }
 private fun playTeachingExample(index:Int){
  if(playing||recordArmed){Toast.makeText(this,"Сначала STOP: пример звучит отдельно от проекта",Toast.LENGTH_LONG).show();return}
  try{val example=TrainingAudio.example(index,File(cacheDir,"teaching"));browserPreview.play(Uri.fromFile(example),onError={msg->Toast.makeText(this,msg,Toast.LENGTH_LONG).show()})}catch(e:Exception){Toast.makeText(this,"Не удалось включить пример: ${e.message}",Toast.LENGTH_LONG).show()}
 }
 private fun addTeachingKit(){
  if(playing||recordArmed){Toast.makeText(this,"Сначала STOP",Toast.LENGTH_SHORT).show();return}
  val target=(listOf(bank)+(0..3).filter{it!=bank}).firstOrNull{b->pads[b].count{it==null}>=4}
  if(target==null){Toast.makeText(this,"Для набора нужны 4 свободных пэда в одном банке. Существующие звуки не заменены",Toast.LENGTH_LONG).show();return}
  try{val free=(0..15).filter{pads[target][it]==null}.take(4);val kit=TrainingAudio.kit(File(filesDir,"training_kit"));kit.forEachIndexed{i,item->val slot=free[i];pads[target][slot]=PadSample(pool.load(item.second.absolutePath,1),item.first,item.second.absolutePath);padVolumes[target][slot]=1f;padPitch[target][slot]=0};bank=target;pad=free.first();savePads();saveVolumes();saveProject();refreshPads();Toast.makeText(this,"Учебные звуки: BANK ${'A'+bank}, пэды ${free.joinToString{(it+1).toString()}}",Toast.LENGTH_LONG).show()}catch(e:Exception){Toast.makeText(this,e.message,Toast.LENGTH_LONG).show()}
 }
'''
replace(' private fun velocity(i:Int)',block+'\n private fun velocity(i:Int)')
# Match REC/PLAY/OVERDUB semantics rather than teaching an immediate REC shortcut.
old=next(line for line in s.splitlines() if line.startswith(' private fun record(od:Boolean)'))
replace(old,''' private fun record(od:Boolean){displayOverride=null;if(playing){if(od){overdub=!overdub;recording=overdub;recordArmed=false;refreshStatus()}else showDisplayMessage("REC • сначала STOP, затем REC → PLAY");return};countInToken++;recordArmed=true;overdub=od;recording=false;passRecorded=false;passOverdub=false;showDisplayMessage(if(od)"OVERDUB готов • нажми PLAY" else "REC готов • нажми PLAY")}''')
old=next(line for line in s.splitlines() if line.startswith(' private fun play(){'))
replace(old,''' private fun play(){if(playing)return;displayOverride=null;countInToken++;val armed=recordArmed;val token=countInToken;val begin={if(armed&&!overdub)clearCurrentTrackForRecord();recordArmed=false;recording=armed;playing=true;startMs=SystemClock.elapsedRealtime()-(pausedTick*(60000.0/bpm/96.0)).toLong();startClock();saveProject();refreshStatus()};if(armed&&countIn)countDown(4,token,begin)else begin()}''')
old=next(line for line in s.splitlines() if line.startswith(' private fun stopTransport(){'))
replace(old,''' private fun stopTransport(){displayOverride=null;countInToken++;if(playing)pausedTick=tick();val didRecord=passRecorded;val didOverdub=passOverdub;playing=false;recording=false;recordArmed=false;overdub=false;lastClockTick=-1;h.removeCallbacks(clock);stopRepeat();if(didRecord)report("recorded_pass");if(didOverdub)report("overdub_pass");report(if(didOverdub&&!didRecord)"overdub_stop" else "stop");passRecorded=false;passOverdub=false;saveProject();refreshStatus()}''')
# Give phone users an explicit equivalent of PLAY START without adding a small row.
replace('setOnClickListener{click()}}','setOnClickListener{click()};if(label=="PLAY"){setOnLongClickListener{playing=false;recording=false;h.removeCallbacks(clock);pausedTick=0L;play();Toast.makeText(this,"PLAY START • с начала",Toast.LENGTH_SHORT).show();true}}}')
# Fresh record starts at the beginning; resumable ordinary PLAY retains STOP position.

replace('recordedCycle[e]=loopCycle();saveProject()', 'recordedCycle[e]=loopCycle();if(overdub)passOverdub=true else passRecorded=true;saveProject()')
# After one loop of REC, the hardware switches automatically to overdubbing.
replace('val t=tick();val beat=t/96;', 'val t=tick();if(recording&&!overdub&&loopCycle()>0)overdub=true;val beat=t/96;')
# Live TC or Swing must not accidentally echo the just recorded events a second time.
replace('events[i]=e.copy(tick=q);changed++', 'val moved=e.copy(tick=q);val born=recordedCycle.remove(e);events[i]=moved;if(born!=null)recordedCycle[moved]=born;changed++')
# Dedicated volume buttons are real applied mixer changes too.
replace('padVolumes[bank][pad]=v/100f;saveVolumes();return v', 'val old=padVolumes[bank][pad];padVolumes[bank][pad]=v/100f;saveVolumes();if(old!=padVolumes[bank][pad])report("mixer_changed");return v')
# Show armed state without falsely claiming playback has begun.
replace('status.text=displayOverride?:workspaceText()', 'status.text=displayOverride?:workspaceText()')
p.write_text(s)
# The course uses explicit completed recording passes, not pre-existing event counts.
p=Path('app/src/main/java/com/bytbeat/mpctrainer/LearnEngine.kt');s=p.read_text()
s=s.replace('fun reset(s:Snapshot):Result', 'fun selectLesson(index:Int,s:Snapshot){lesson=index.coerceIn(0,LAST_LESSON);begin(s)}\n fun reset(s:Snapshot):Result',1)
s=s.replace('if(action=="stop")challengeRecorded=challengeRecorded||s.eventCount>0','if(action=="recorded_pass")challengeRecorded=true')
s=s.replace('if(action=="overdub_stop")challengeOverdub=challengeOverdub||s.eventCount>0','if(action=="overdub_pass")challengeOverdub=true')
s=s.replace('3->action=="stop"&&s.eventCount>baselineEvents;4->action=="overdub_stop"&&s.eventCount>baselineEvents','3->action=="recorded_pass"&&s.eventCount>0;4->action=="overdub_pass"&&s.eventCount>0')
s=s.replace('REC → настучи короткий ритм → STOP','REC → PLAY → настучи короткий ритм → STOP').replace('OVERDUB → добавь ещё один слой → STOP','OVERDUB → PLAY → добавь ещё один слой → STOP')
p.write_text(s)
# Update existing tests to simulate actual completed passes instead of STOP tokens.
p=Path('app/src/test/java/com/bytbeat/mpctrainer/LearnEngineTest.kt');s=p.read_text()
s=s.replace('e.report("stop",s(pads=1,events=3))','e.report("recorded_pass",s(pads=1,events=3))')
s=s.replace('e.report("stop",s(pads=2,events=2))','e.report("recorded_pass",s(pads=2,events=2))')
s=s.replace('e.report("overdub_stop",','e.report("overdub_pass",')
p.write_text(s)
print('Explanatory course, audible examples, understanding checks, practice kit and hardware transport behavior integrated')

from pathlib import Path

p=Path('app/src/main/java/com/bytbeat/mpctrainer/LearnEngine.kt')
s=p.read_text()
repls={
'private var challengeClean=false;private var challengeRecorded=false;private var challengeOverdub=false;private var challengeTc=false;private var challengeTrack=false;private var challengeStep=false;private var challengeMix=false;private var challengeSaved=false':'private var challengeClean=false;private var challengeRecorded=false;private var challengeOverdub=false;private var challengeTc=false;private var challengeTrack=false;private var challengeStep=false;private var challengeMix=false;private var challengeSaved=false',
'14->action=="mixer_changed";else->false':'14->action=="mixer_changed";15->action=="pitch_changed";16->action=="tc_do_it";else->false',
'14->"15 • Mixer / Volume";else->"Финальное задание"':'14->"15 • Mixer / Volume";15->"16 • Pitch";16->"17 • Timing Correct / DO IT";else->"Финальное задание"',
'14->"MIXER / VOLUME нужен для баланса: громкость выбранного пэда меняется отдельно, а MASTER / PROJECT управляет общей громкостью проекта, сохраняя соотношение между пэдами.";else->"Собери небольшой бит с чистого проекта без пошаговых подсказок. Тренажёр проверит реальные действия, а не результат предыдущих уроков."':'14->"MIXER / VOLUME нужен для баланса: громкость выбранного пэда меняется отдельно, а MASTER / PROJECT управляет общей громкостью проекта, сохраняя соотношение между пэдами.";15->"PITCH меняет высоту выбранного пэда по полутонам. В тренажёре диапазон ±12 st: звук реально репитчится, а PITCH RESET возвращает 0 st.";16->"TIMING CORRECT выравнивает события по выбранной сетке. TRIPLET даёт триольную сетку, SWING меняет грув, SHIFT TIMING сдвигает результат в ticks, а DO IT применяет настройки к уже записанному текущему TRACK.";else->"Собери небольшой бит с чистого проекта без пошаговых подсказок. Тренажёр проверит реальные действия, а не результат предыдущих уроков."',
'14->"ЗАДАНИЕ • MENU → MIXER / VOLUME → измени громкость выбранного пэда или MASTER → APPLY.";else->"ЭКЗАМЕН • Сначала NEW PROJECT. Без подсветки: загрузи 2+ звука → REC → OVERDUB → измени TC → другой TRACK → добавь шаг в Step Seq → выставь баланс в MIXER / VOLUME → SAVE."':'14->"ЗАДАНИЕ • MENU → MIXER / VOLUME → измени громкость выбранного пэда или MASTER → APPLY.";15->"ЗАДАНИЕ • Выбери пэд со звуком → нажми PITCH + или PITCH − → послушай изменение → MENU → PITCH RESET.";16->"ЗАДАНИЕ • Открой TC → выбери TIME DIVISION → при желании TRIPLET/SWING/SHIFT TIMING → нажми DO IT, чтобы применить TC к уже записанному TRACK.";else->"ЭКЗАМЕН • Сначала NEW PROJECT. Без подсветки: загрузи 2+ звука → REC → OVERDUB → измени TC → другой TRACK → добавь шаг в Step Seq → выставь баланс в MIXER / VOLUME → SAVE."',
'14->"Недостаточно просто открыть MIXER / VOLUME — измени уровень и нажми APPLY.";else->prompt()':'14->"Недостаточно просто открыть MIXER / VOLUME — измени уровень и нажми APPLY.";15->"Нажми PITCH + или PITCH − на выбранном пэде. После прослушивания можешь вернуть 0 st через PITCH RESET.";16->"Открой TC и нажми DO IT: урок проверяет применение Timing Correct к уже записанным событиям, а не просто открытие окна.";else->prompt()',
'companion object{const val LAST_LESSON=15}':'companion object{const val LAST_LESSON=17}'
}
for old,new in repls.items():
    if old not in s: raise SystemExit('LearnEngine anchor missing: '+old[:70])
    s=s.replace(old,new,1)
p.write_text(s)

p=Path('app/src/main/java/com/bytbeat/mpctrainer/MpcBasics.kt')
s=p.read_text()
s=s.replace('ControlGuide("TIMING CORRECT","Выравнивает записанные удары по ритмической сетке.","Сравни сетки 1/8, 1/16 и 1/32."),','ControlGuide("TIMING CORRECT","Выравнивает события по сетке. Поддерживает обычную/триольную сетку, SWING и SHIFT TIMING; DO IT применяет настройки к уже записанному TRACK.","Запиши неровный рисунок, выбери сетку и примени DO IT, затем сравни результат."),')
s=s.replace('ControlGuide("16 LEVELS","В этом базовом тренажёре раскладывает один выбранный звук по 16 уровням velocity.","Выбери один звук и послушай, как velocity возрастает по пэдам."),','ControlGuide("16 LEVELS","Раскладывает выбранный звук по 16 значениям параметра. В тренажёре изучаем базовые VELOCITY и TUNE; TUNE использует ORIGINAL PAD как исходную высоту.","Сравни VELOCITY, затем TUNE и найди ORIGINAL PAD."),\n  ControlGuide("PITCH − / PITCH +","Меняет высоту выбранного пэда по полутонам; в тренажёре ±12 st.","Измени PITCH выбранного сэмпла, послушай результат и верни 0 st через PITCH RESET."),')
s=s.replace('BasicLesson("5. Тайминг и грув","На слух пойми работу TIMING CORRECT и SWING.",listOf("TIMING CORRECT","SWING")),','BasicLesson("5. Тайминг и грув","На слух пойми TIMING CORRECT, TRIPLET, SWING, SHIFT TIMING и применение DO IT к записанному TRACK.",listOf("TIMING CORRECT","SWING")),')
s=s.replace('BasicLesson("6. Инструменты пэдов","Разберись с FULL LEVEL и 16 LEVELS по velocity.",listOf("FULL LEVEL","16 LEVELS")),','BasicLesson("6. Инструменты пэдов","Разберись с FULL LEVEL, 16 LEVELS VELOCITY/TUNE и простым PITCH выбранного пэда.",listOf("FULL LEVEL","16 LEVELS","PITCH − / PITCH +")),')
p.write_text(s)

p=Path('app/src/test/java/com/bytbeat/mpctrainer/LearnEngineTest.kt')
s=p.read_text()
s=s.replace('assertEquals(LearnEngine.LAST_LESSON,e.lesson)','assertEquals(15,e.lesson)')
insert='''\n    @Test fun pitchAndTcDoItLessonsRequireActualActions(){val pitch=LearnEngine(15);pitch.begin(s(pads=1));assertFalse(pitch.report("pad",s(pads=1)).advanced);assertTrue(pitch.report("pitch_changed",s(pads=1)).advanced);assertEquals(16,pitch.lesson);val tc=LearnEngine(16);tc.begin(s(pads=1,events=2));assertFalse(tc.report("tc",s(pads=1,events=2,tc=32)).advanced);assertTrue(tc.report("tc_do_it",s(pads=1,events=2,tc=32)).advanced);assertEquals(LearnEngine.LAST_LESSON,tc.lesson)}\n'''
needle='\n    @Test fun challengeRequiresCoreWorkflowFromCleanProject()'
if needle not in s: raise SystemExit('LearnEngineTest insertion anchor missing')
s=s.replace(needle,insert+needle,1)
p.write_text(s)

print('Pitch and full Timing Correct Learn lessons integrated')

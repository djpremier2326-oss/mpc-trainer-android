package com.bytbeat.mpctrainer

data class ControlGuide(val name:String,val purpose:String,val practice:String)
data class BasicLesson(val title:String,val goal:String,val controls:List<String>)

object MpcBasics {
 val controls=listOf(
  ControlGuide("MAIN","Возвращает на главный рабочий экран с текущей Sequence, Track и транспортом.","Возвращайся сюда после BROWSE или SAMPLE EDIT."),
  ControlGuide("BROWSE","Позволяет найти и прослушать WAV/MP3, а затем загрузить звук на выбранный пэд.","Выбери звук, прослушай его и загрузи на пэд."),
  ControlGuide("BANK A–D","Переключает четыре банка по 16 пэдов.","Загрузи звуки в BANK A, затем переключай банки и проверь, что назначения сохраняются."),
  ControlGuide("PADS 1–16","Запускают назначенные звуки и записывают удары в Sequence.","Сыграй kick, snare и hi-hat с пэдов."),
  ControlGuide("BPM / TEMPO","Задаёт темп проекта. От BPM зависят воспроизведение, запись, METRONOME и NOTE REPEAT.","Поставь медленный BPM, сыграй под METRONOME, затем сравни с более быстрым BPM."),
  ControlGuide("REC","Начинает новую запись на текущем TRACK.","Запиши простой двухтактовый рисунок ударных."),
  ControlGuide("OVERDUB","Добавляет новые удары поверх уже записанного рисунка, не стирая его.","Добавь hi-hat поверх kick и snare."),
  ControlGuide("PLAY / STOP","Запускает и останавливает воспроизведение Sequence.","Прослушай рисунок и останови его кнопкой STOP."),
  ControlGuide("METRONOME","Даёт ровный ориентир темпа во время записи и воспроизведения.","Включи его и играй под клик в BPM проекта."),
  ControlGuide("COUNT IN","Даёт четыре счётных удара перед началом записи.","Включи COUNT IN, нажми REC и начни играть после отсчёта."),
  ControlGuide("TIMING CORRECT","Выравнивает записанные удары по ритмической сетке.","Сравни сетки 1/8, 1/16 и 1/32."),
  ControlGuide("SWING","Сдвигает чередующиеся позиции сетки и меняет ощущение грува.","Прослушай один рисунок без SWING и затем со SWING."),
  ControlGuide("NOTE REPEAT","Повторяет удерживаемый пэд с выбранным ритмическим делением.","Удерживай пэд hi-hat и сделай ровную серию повторов."),
  ControlGuide("FULL LEVEL","Заставляет пэды играть с максимальной velocity.","Включи FULL LEVEL и сравни удар с обычным нажатием в тренажёре."),
  ControlGuide("16 LEVELS","В этом базовом тренажёре раскладывает один выбранный звук по 16 уровням velocity.","Выбери один звук и послушай, как velocity возрастает по пэдам."),
  ControlGuide("STEP SEQ","Позволяет программировать ритм пошагово.","Расставь kick и snare по сетке."),
  ControlGuide("TRACK","Выбирает текущую дорожку внутри Sequence.","Запиши второй слой на другом TRACK."),
  ControlGuide("TRACK MUTE","Заглушает TRACK, не удаляя его события.","Во время воспроизведения выключи и снова включи один слой."),
  ControlGuide("SAMPLE EDIT","Работает с загруженным WAV или MP3: задаёт Start/End и делает простую равномерную нарезку. MP3 внутри декодируется для редактирования.","Загрузи музыкальный MP3, выбери нужный участок, сделай CHOP и сыграй слайсы с соседних пэдов."),
  ControlGuide("SAVE / PROJECT","Сохраняет назначения пэдов, записанные события и основные настройки проекта для следующего запуска.","Сохрани проект, закрой приложение и снова открой его."),
  ControlGuide("NEW PROJECT","Возвращает тренажёр к пустому проекту, не удаляя импортированные исходные аудиофайлы с телефона.","Используй перед финальным экзаменом, чтобы собрать бит с нуля.")
 )
 val lessons=listOf(
  BasicLesson("1. Знакомство с MPC","Узнай основные группы управления и назначение базовых кнопок.",listOf("MAIN","BROWSE","BANK A–D","PADS 1–16","BPM / TEMPO","REC","OVERDUB","PLAY / STOP")),
  BasicLesson("2. От BROWSE к пэдам","Найди, прослушай и назначь звуки.",listOf("BROWSE","BANK A–D","PADS 1–16")),
  BasicLesson("3. Первая Sequence","Выставь темп, запиши и прослушай простой двухтактовый бит.",listOf("BPM / TEMPO","COUNT IN","METRONOME","REC","PLAY / STOP","PADS 1–16")),
  BasicLesson("4. Добавляем слой","Добавь hi-hat, не стирая первый проход.",listOf("OVERDUB","NOTE REPEAT")),
  BasicLesson("5. Тайминг и грув","На слух пойми работу TIMING CORRECT и SWING.",listOf("TIMING CORRECT","SWING")),
  BasicLesson("6. Инструменты пэдов","Разберись с FULL LEVEL и 16 LEVELS по velocity.",listOf("FULL LEVEL","16 LEVELS")),
  BasicLesson("7. Пошаговая запись","Собери и поправь простой рисунок на сетке STEP SEQ.",listOf("STEP SEQ")),
  BasicLesson("8. Дорожки","Используй другой TRACK и разберись с TRACK MUTE.",listOf("TRACK","TRACK MUTE")),
  BasicLesson("9. Работа с сэмплом","Загрузи WAV/MP3, выставь Start/End и нарежь музыкальный сэмпл на пэды.",listOf("BROWSE","SAMPLE EDIT")),
  BasicLesson("10. Бит самостоятельно","Начни с пустого проекта и без подсказок закончи рабочий сохранённый бит.",listOf("NEW PROJECT","BROWSE","PADS 1–16","BPM / TEMPO","REC","OVERDUB","TIMING CORRECT","TRACK","STEP SEQ","SAVE / PROJECT"))
 )
 val intentionallyAdvanced=listOf("Глубокая MIDI-маршрутизация и настройка","Подробное редактирование MIDI-нот","Продвинутая автоматизация","Probability, ratchet и micro-nudge","Продвинутая настройка Q-Link","Глубокий синтез и программирование плагинов")
}

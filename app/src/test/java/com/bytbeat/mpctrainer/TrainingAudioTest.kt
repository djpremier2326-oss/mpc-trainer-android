package com.bytbeat.mpctrainer

import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class TrainingAudioTest {
 @Test fun everyLessonHasPlayablePcmExampleAndUnderstandableContent(){
  val dir=Files.createTempDirectory("lessons").toFile()
  try{
   assertEquals(LearnEngine.LAST_LESSON+1,LessonContent.lessons.size)
   LessonContent.lessons.forEachIndexed{i,lesson->
    assertTrue(lesson.meaning.length>100);assertTrue(lesson.purpose.isNotBlank());assertTrue(lesson.listen.isNotBlank());assertTrue(lesson.practice.isNotBlank())
    assertTrue(lesson.correct in lesson.answers.indices);assertEquals(lesson.answers.size,lesson.answers.distinct().size)
    val info=WavSlicer.inspect(TrainingAudio.example(i,dir));assertEquals(16,info.bits);assertEquals(22050,info.sampleRate);assertTrue(info.frames>info.sampleRate*8)
   }
  }finally{dir.deleteRecursively()}
 }
 @Test fun kitCanBeSlicedWithoutExternalFiles(){
  val dir=Files.createTempDirectory("kit").toFile()
  try{
   val kit=TrainingAudio.kit(dir);assertEquals(4,kit.size)
   kit.forEach{assertTrue(WavSlicer.inspect(it.second).frames>100)}
   val cuts=WavSlicer.chop(kit.last().second,dir.resolve("chops"),0f,1f,4,"test")
   assertEquals(4,cuts.size);assertTrue(cuts.all{WavSlicer.inspect(it).frames>0})
  }finally{dir.deleteRecursively()}
 }
}

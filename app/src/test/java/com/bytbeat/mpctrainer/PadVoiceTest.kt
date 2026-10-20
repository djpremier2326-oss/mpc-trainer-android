package com.bytbeat.mpctrainer

import org.junit.Assert.*
import org.junit.Test

class PadVoiceTest {
 @Test fun liveAndRecordedEventRenderTheSamePcmIncludingVelocityAndTune(){
  val source=PcmMixer.Sample(FloatArray(4096){kotlin.math.sin(it*.05).toFloat()*.4f},1,48000)
  for(velocity in listOf(8,64,100,127))for(offset in listOf(-15,-3,0,7,15)){
   val captured=PadVoice.parameters(velocity,.9f,.8f,-2,offset)
   val live=PcmMixer();val recorded=PcmMixer()
   live.play(source,captured.gain,captured.gain,captured.rate)
   val replay=PadVoice.parameters(velocity,.9f,.8f,-2,offset)
   recorded.play(source,replay.gain,replay.gain,replay.rate)
   assertArrayEquals(live.render(1024).first,recorded.render(1024).first,0f)
  }
 }
 @Test fun sixteenTunePadsAreDistinctAndOriginalPadKeepsBasePitch(){
  val rates=(0..15).map{PadVoice.parameters(127,1f,1f,0,it).rate}
  assertEquals(16,rates.distinct().size);assertEquals(1f,rates[0],0f)
  assertEquals(2f,PadVoice.parameters(127,1f,1f,12,0).rate,0f)
 }
 @Test fun onlyLegacyFixedBaselineIsMigrated(){
  assertEquals(127,PadVoice.restoredVelocity(100,1))
  assertEquals(100,PadVoice.restoredVelocity(100,2))
  for(v in listOf(8,64,95,103,127))assertEquals(v,PadVoice.restoredVelocity(v,1))
 }
 @Test fun unityHasNoHiddenGainAndBothVolumeControlsMultiply(){
  assertEquals(1f,PadVoice.parameters(127,1f,1f,0).gain,0f)
  assertEquals(.1f,PadVoice.parameters(127,.5f,.2f,0).gain,.00001f)
 }
}

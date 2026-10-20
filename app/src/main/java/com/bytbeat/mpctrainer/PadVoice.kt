package com.bytbeat.mpctrainer

/** The same event parameters drive live monitoring and sequencer playback. */
object PadVoice {
 data class Parameters(val gain:Float,val rate:Float)
 fun parameters(velocity:Int,padVolume:Float,masterVolume:Float,padPitch:Int,tuneOffset:Int=0)=Parameters(
  TrainerPlayback.gain(velocity,padVolume,masterVolume),
  Math.pow(2.0,(padPitch.coerceIn(-12,12)+tuneOffset.coerceIn(-15,15))/12.0).toFloat().coerceIn(.125f,8f)
 )
 fun restoredVelocity(value:Int,schema:Int):Int=if(schema<2&&value==100)127 else value.coerceIn(1,127)
}

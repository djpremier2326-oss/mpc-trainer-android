package com.bytbeat.mpctrainer

/** Small explicit bridge between the trainer engine and the phone hardware surface. */
data class TrainerUiSnapshot(
    val bank:Int=0,
    val pad:Int=0,
    val track:Int=0,
    val playing:Boolean=false,
    val recording:Boolean=false,
    val overdub:Boolean=false,
    val noteRepeat:Boolean=false,
    val fullLevel:Boolean=false,
    val levels16:Boolean=false,
    val trackMuted:Boolean=false,
    val activeBank:Int=-1,
    val activePad:Int=-1,
    val activeSerial:Long=0L
)

object TrainerUiState {
    @Volatile var snapshot=TrainerUiSnapshot()
        private set

    @Synchronized fun publish(
        bank:Int,pad:Int,track:Int,playing:Boolean,recording:Boolean,overdub:Boolean,
        noteRepeat:Boolean,fullLevel:Boolean,levels16:Boolean,trackMuted:Boolean
    ) {
        val old=snapshot
        snapshot=old.copy(bank=bank,pad=pad,track=track,playing=playing,recording=recording,
            overdub=overdub,noteRepeat=noteRepeat,fullLevel=fullLevel,levels16=levels16,
            trackMuted=trackMuted)
    }

    @Synchronized fun pulsePad(bank:Int,pad:Int) {
        val old=snapshot
        snapshot=old.copy(activeBank=bank,activePad=pad,activeSerial=old.activeSerial+1)
    }
}
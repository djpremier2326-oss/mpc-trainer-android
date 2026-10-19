package com.bytbeat.mpctrainer

import org.junit.Assert.*
import org.junit.Test

class TrainerUiStateTest {
    @Test fun publishPreservesLatestPadPulse() {
        val before=TrainerUiState.snapshot.activeSerial
        TrainerUiState.pulsePad(2,7)
        val pulsed=TrainerUiState.snapshot
        assertEquals(2,pulsed.activeBank)
        assertEquals(7,pulsed.activePad)
        assertEquals(before+1,pulsed.activeSerial)

        TrainerUiState.publish(1,3,2,true,false,false,true,false,true,true)
        val after=TrainerUiState.snapshot
        assertEquals(pulsed.activeSerial,after.activeSerial)
        assertEquals(2,after.activeBank)
        assertEquals(7,after.activePad)
        assertEquals(1,after.bank)
        assertEquals(2,after.track)
        assertTrue(after.playing)
        assertTrue(after.noteRepeat)
        assertTrue(after.levels16)
        assertTrue(after.trackMuted)
    }

    @Test fun everyPadPulseGetsANewSerial() {
        val start=TrainerUiState.snapshot.activeSerial
        TrainerUiState.pulsePad(0,0)
        TrainerUiState.pulsePad(0,0)
        assertEquals(start+2,TrainerUiState.snapshot.activeSerial)
    }
}

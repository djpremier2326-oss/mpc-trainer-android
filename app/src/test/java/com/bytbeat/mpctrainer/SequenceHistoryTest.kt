package com.bytbeat.mpctrainer

import org.junit.Assert.*
import org.junit.Test

class SequenceHistoryTest {
    @Test
    fun snapshotsUndoRedoWithoutSharingMutableLists() {
        val h = SequenceHistory<Int>()
        val notes = mutableListOf(1)
        h.checkpoint(notes)
        notes.add(2)
        assertEquals(listOf(1), h.undo(notes))
        assertEquals(listOf(1, 2), h.redo(listOf(1)))
    }

    @Test
    fun newEditInvalidatesRedo() {
        val h = SequenceHistory<Int>()
        h.checkpoint(listOf(1))
        h.undo(listOf(1, 2))
        h.checkpoint(listOf(1))
        assertNull(h.redo(listOf(1, 3)))
    }

    @Test
    fun unchangedEditsDoNotHideThePreviousChange() {
        val h = SequenceHistory<Int>()
        h.checkpoint(listOf(1))
        h.checkpoint(listOf(1, 2))
        assertEquals(listOf(1), h.undo(listOf(1, 2)))
    }

    @Test
    fun historyIsBounded() {
        val h = SequenceHistory<Int>(2)
        h.checkpoint(listOf(1))
        h.checkpoint(listOf(2))
        h.checkpoint(listOf(3))
        assertEquals(listOf(3), h.undo(listOf(4)))
        assertEquals(listOf(2), h.undo(listOf(3)))
        assertNull(h.undo(listOf(2)))
    }
}

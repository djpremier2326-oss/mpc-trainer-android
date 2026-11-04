package com.bytbeat.mpctrainer

/** Bounded, immutable snapshots: one entry per musical edit, never per scheduler tick. */
class SequenceHistory<T>(private val limit: Int = 32) {
    private val undo = java.util.ArrayDeque<List<T>>()
    private val redo = java.util.ArrayDeque<List<T>>()

    fun checkpoint(current: List<T>) {
        val snapshot = current.toList()
        if (undo.peekLast() != snapshot) undo.addLast(snapshot)
        while (undo.size > limit) undo.removeFirst()
        redo.clear()
    }

    fun undo(current: List<T>): List<T>? {
        while (undo.peekLast() == current) undo.removeLast()
        if (undo.isEmpty()) return null
        redo.addLast(current.toList())
        return undo.removeLast()
    }

    fun redo(current: List<T>): List<T>? {
        if (redo.isEmpty()) return null
        undo.addLast(current.toList())
        return redo.removeLast()
    }
}

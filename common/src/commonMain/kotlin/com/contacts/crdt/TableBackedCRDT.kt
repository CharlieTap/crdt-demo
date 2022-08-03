package com.contacts.crdt

import com.benasher44.uuid.Uuid
import com.github.michaelbull.result.getOrThrow
import com.tap.hlc.HybridLogicalClock
import com.tap.hlc.HybridLogicalClock.Companion.increment
import kotlin.reflect.KProperty

interface TableBackedCRDT {
    val id: Uuid
    val namespace: String

    /**
     * Takes the local clock and mutator function which returns a set of mutations
     * Runs each mutation sequentially using the output clock of the last mutation
     *
     */
    fun mutate(clock: HybridLogicalClock, mutator: CRDTMutator) : LinkedHashSet<CRDTDelta> {
        return with(this@TableBackedCRDT) {
            mutator().fold(linkedSetOf()) { acc, mutation ->
                val modifiedClock = if(acc.isEmpty()) {
                    clock
                } else acc.last().timestamp
                acc.apply { add(mutation(modifiedClock)) }
            }
        }
    }
}

context(TableBackedCRDT)
fun <T> KProperty<T>.mutate(value: T) : (HybridLogicalClock) -> CRDTDelta {
    return { clock ->
        CRDTDelta(
            namespace,
            id,
            increment(clock).getOrThrow { IllegalStateException() },
            this.name,
            value.toString()
        )
    }
}
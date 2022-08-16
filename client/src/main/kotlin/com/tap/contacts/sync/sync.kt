package com.tap.contacts.sync

import androidx.work.*
import com.benasher44.uuid.Uuid
import com.contacts.crdt.CRDTDelta
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getOr
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.getOrThrow
import com.tap.contacts.Contact
import com.tap.contacts.ContactQueries
import com.tap.hlc.HybridLogicalClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties

object Sync {
    private val changeName : Contact.(String) -> Contact = {copy(name = it)}
    private val changeEmail : Contact.(String) -> Contact = {copy(email = it)}
    private val changePhone : Contact.(String) -> Contact = {copy(phone = it)}
    private val changeTombstone : Contact.(String) -> Contact = {copy(tombstone = it)}

    val CONTACT_MUTATORS = setOf(
        Triple(Contact::name, Contact::name_timestamp, changeName),
        Triple(Contact::email, Contact::email_timestamp, changeEmail),
        Triple(Contact::phone, Contact::phone_timestamp, changePhone),
        Triple(Contact::tombstone, Contact::tombstone_timestamp, changeTombstone),
    )
}


fun Contact.mutate(
    delta: CRDTDelta,
    mutators: Set<Triple<KProperty<String>, KProperty<String>, Contact.(String) -> Contact>>
) : Contact {

    return mutators.fold(this@mutate) { contact, mutator ->
        if (delta.column == mutator.first.name) {
            val encoded = mutator.second.getter.call(contact)
            val hlc = if(encoded.isNotEmpty()) {
                HybridLogicalClock.decodeFromString(encoded).get()
            } else null
            if (hlc == null || delta.timestamp > hlc) {
                mutator.third(contact, delta.value)
            } else contact
        } else contact
    }
}

fun mutate(existingContactMap: Map<String, Contact>, deltas: Set<CRDTDelta>) : Set<Contact> {
    val groupedDeltas = deltas.groupBy { it.id.toString() }
    return groupedDeltas.mapNotNull { entry ->

        val contact = existingContactMap[entry.key] ?: Contact(entry.key, "", "", "", "", "","", "", "")

        entry.value.fold(contact) { acc, delta ->
            acc.mutate(delta, Sync.CONTACT_MUTATORS)
        }.let { mutatedContact ->
            if (contact != mutatedContact) {
                mutatedContact
            } else null
        }
    }.toSet()
}

/**
 * Run this every time time we receive events from external nodes
 */
suspend fun syncClock(localClockStream: MutableStateFlow<HybridLogicalClock>, events: Set<CRDTDelta>) : MutableStateFlow<HybridLogicalClock> {
    return events.fold(localClockStream.value) { acc, event ->
        HybridLogicalClock.remoteTock(localClockStream.value, event.timestamp).getOr(acc)
    }.let {syncedClock ->
        localClockStream.emit(syncedClock)
        localClockStream
    }
}




suspend fun syncData(queries: ContactQueries, deltas: Set<CRDTDelta>) {

    val contactMap = queries.whereIn(deltas.map{ it.id.toString()})
        .executeAsList()
        .associateBy { it.id }

    mutate(contactMap, deltas).forEach { contact ->
        queries.upsert(contact)
    }
}

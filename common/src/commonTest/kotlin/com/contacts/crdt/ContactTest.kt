package com.contacts.crdt

import com.tap.hlc.HybridLogicalClock
import com.tap.hlc.Timestamp
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ContactTest {

    private val now: Timestamp = Timestamp.now(Clock.System)
    private val earlier: Timestamp = Timestamp(now.epochMillis - (1000 * 60 * 60))

    private val localNow = HybridLogicalClock(now)
    private val localEarlier = localNow.copy(timestamp = earlier)

    private val contact = Contact("Bob Sapp", 555_266_336, "bob.sapp@bob.com")


    @Test
    fun `creating two contacts should yield different ids`() {
        val contact2 = Contact("Bob Sadpp", 555_266_336, "bob.sadpp@bob.com")
        assertNotEquals(contact.id, contact2.id)
    }

    @Test
    fun `mutating a contact should yield a set of CRDT deltas`() {

        val mutatedName = "Ben Smith"
        val mutatedPhone = 554_667_897

        val deltas = contact.mutate(localEarlier) {
            linkedSetOf(
                contact::name.mutate(mutatedName),
                contact::phone.mutate(mutatedPhone)
            )
        }

        deltas.forEach(::println)

        assertTrue(deltas.size == 2)

        assertEquals(contact.namespace, deltas.elementAt(0).namespace)
        assertEquals(contact.id, deltas.elementAt(0).id)
        assertEquals(contact::name.name, deltas.elementAt(0).column.name)
        assertTrue(deltas.elementAt(0).timestamp >= localNow)
        assertEquals(mutatedName, deltas.elementAt(0).value)

        assertEquals(contact.namespace, deltas.elementAt(1).namespace)
        assertEquals(contact.id, deltas.elementAt(1).id)
        assertEquals(contact::phone.name, deltas.elementAt(1).column.name)
        assertTrue(deltas.elementAt(1).timestamp >= deltas.elementAt(0).timestamp)
        assertEquals(mutatedPhone, deltas.elementAt(1).value)
    }


}
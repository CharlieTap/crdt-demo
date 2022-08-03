package com.contacts.crdt

import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4

data class Contact(val name: String, val phone: Int, val email: String) : TableBackedCRDT {

    override val namespace: String = "contacts"
    override val id: Uuid = uuid4()

}

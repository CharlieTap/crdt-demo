package com.tap.contacts.viewmodel

import com.tap.contacts.Contact

data class ContactsState(
    val genButtonName: String,
    val contacts: List<Contact>,
) {
    companion object {
        val Default = ContactsState("Generate events",  emptyList())
    }
}
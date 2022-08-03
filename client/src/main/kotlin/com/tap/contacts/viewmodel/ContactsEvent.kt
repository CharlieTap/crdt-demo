package com.tap.contacts.viewmodel

sealed interface ContactsEvent {
    object ToggleEventGeneration : ContactsEvent
}
package com.tap.contacts.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.tap.contacts.Database
import com.tap.contacts.data.ContactsDatastore
import com.tap.contacts.jobs.ContactDeltaGenerator
import com.tap.contacts.jobs.ContactsSync
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val database: Database,
    private val workManager: WorkManager,
    private val datastore: ContactsDatastore
) : MVIViewModel<ContactsState, ContactsEvent, ContactsEffect>()
{
    private val contacts        = database.contactQueries.all().asFlow().mapToList()
    private val eventGeneration = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            while(isActive) {
                ContactsSync.schedule(workManager)
                delay(2000)
            }
        }
    }


    private val _state = contacts.combine(eventGeneration) { contacts, eventGen ->
        val eventGenName = if(eventGen) {
            "Turn off event gen"
        } else {
            "Generate events"
        }

       ContactsState(eventGenName, contacts)
    }
    override val state: StateFlow<ContactsState> = _state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ContactsState.Default)

    override fun handleEvent(event: ContactsEvent) {
        when (event) {
            ContactsEvent.ToggleEventGeneration -> {
                viewModelScope.launch {
                    eventGeneration.emit(eventGeneration.value.not())
                    if(eventGeneration.value) {
                        ContactDeltaGenerator.schedule(workManager)
                    } else {
                        ContactDeltaGenerator.cancel(workManager)
                    }
                }
            }
            ContactsEvent.ResetState -> {
                viewModelScope.launch (Dispatchers.IO){
                    database.contactQueries.wipe()
                    datastore.saveLastSyncId(0)
                }
            }
        }
    }

}

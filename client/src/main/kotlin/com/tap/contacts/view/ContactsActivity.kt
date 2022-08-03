package com.tap.contacts.view

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.collectAsState
import com.tap.contacts.viewmodel.ContactsViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.tap.contacts.viewmodel.ContactsEvent
import kotlinx.coroutines.launch


@AndroidEntryPoint
class ContactsActivity : AppCompatActivity()
{
    private val viewModel by viewModels<ContactsViewModel>()

    private fun eventHandler(event: ContactsEvent) {
        lifecycleScope.launch {
            viewModel.postEvent(event)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            val state = viewModel.state.collectAsState()

            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Button(onClick = { eventHandler(ContactsEvent.ToggleEventGeneration) }) {
                        Text(state.value.genButtonName)
                    }
                }
                LazyColumn {
                    items(state.value.contacts) { contact ->
                        Row(
                            Modifier.height(80.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Card(Modifier.fillMaxWidth().padding(8.dp)) {
                                Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text(contact.name)
                                    }
                                    Column() {
                                        Text(contact.email)
                                        Text(contact.phone)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}
package com.contacts.crdt.network.request

import com.contacts.crdt.CRDTDelta
import kotlinx.serialization.Serializable

@Serializable
data class SyncRequest(val events: Set<CRDTDelta>, val lastSync: Long = 0)

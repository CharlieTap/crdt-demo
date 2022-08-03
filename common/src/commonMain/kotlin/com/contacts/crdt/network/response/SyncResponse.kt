package com.contacts.crdt.network.response

import com.contacts.crdt.CRDTDelta
import kotlinx.serialization.Serializable

@Serializable
data class SyncResponse(val id: Long, val events: Set<CRDTDelta>)
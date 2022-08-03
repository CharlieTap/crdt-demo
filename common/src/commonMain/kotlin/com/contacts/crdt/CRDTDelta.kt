package com.contacts.crdt

import com.benasher44.uuid.Uuid
import com.tap.hlc.HybridLogicalClock
import kotlinx.serialization.Serializable

@Serializable
data class CRDTDelta(
    val namespace: String,
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    @Serializable(with = HLCSerializer::class)
    val timestamp: HybridLogicalClock,
    val column: String,
    val value: String
)

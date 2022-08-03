package com.contacts.crdt

import com.github.michaelbull.result.getOrThrow
import com.tap.hlc.HybridLogicalClock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object HLCSerializer : KSerializer<HybridLogicalClock> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("HybridLogicalClock", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: HybridLogicalClock) {
        val string = HybridLogicalClock.encodeToString(value)
        encoder.encodeString(string)
    }

    override fun deserialize(decoder: Decoder): HybridLogicalClock {
        val string = decoder.decodeString()
        return HybridLogicalClock.decodeFromString(string).getOrThrow { SerializationException("Failed to deserialize HLC") }
    }
}
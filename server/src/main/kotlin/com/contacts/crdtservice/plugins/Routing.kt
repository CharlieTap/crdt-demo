package com.contacts.crdtservice.plugins

import com.benasher44.uuid.Uuid
import com.contacts.crdt.CRDTDelta
import com.contacts.crdtservice.Database
import com.contacts.crdt.network.request.SyncRequest
import com.contacts.crdt.network.response.SyncResponse
import com.github.michaelbull.result.getOrThrow
import com.tap.hlc.HybridLogicalClock
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*

fun Application.configureRouting(database: Database) {

    routing {
        get("/") {
            call.respondText("Hello World from the Contacts CRDT service")
        }
    }

    routing {
        post("/sync") {
            val request = call.receive<SyncRequest>()

            val queries = database.contactDeltaEventSourceQueries
            request.events.forEach { delta ->
                queries.insert(
                    delta.id.toString(),
                    delta.column,
                    delta.value,
                    HybridLogicalClock.encodeToString(delta.timestamp)
                )
            }

            val queriedEvents = queries.eventsSince(request.lastSync).executeAsList()
            val events = queriedEvents.map {
                CRDTDelta(
                    "contacts",
                    Uuid.fromString(it.crdt_id),
                    HybridLogicalClock.decodeFromString(it.timestamp).getOrThrow { IllegalStateException() },
                    it.column,
                    it.value_
                )
            }.filter { delta ->
                delta.id !in request.events.map(CRDTDelta::id)
            }

            call.respond(SyncResponse(queriedEvents.lastOrNull()?.id ?: request.lastSync, events.toSet()))
        }
    }
}

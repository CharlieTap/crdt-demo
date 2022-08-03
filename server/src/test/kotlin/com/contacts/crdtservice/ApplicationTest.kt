package com.contacts.crdtservice

import com.benasher44.uuid.uuid4
import com.contacts.crdt.CRDTDelta
import com.contacts.crdtservice.plugins.configureDatabase
import com.contacts.crdtservice.plugins.configureRouting
import com.contacts.crdtservice.plugins.configureSerialization
import com.contacts.crdt.network.request.SyncRequest
import com.tap.hlc.HybridLogicalClock
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json

import io.ktor.server.testing.testApplication
import kotlin.test.assertEquals
import kotlin.test.Test
import kotlin.test.assertTrue


class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        application {
            configureRouting(configureDatabase())
        }
        client.get("/").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("Hello World from the Contacts CRDT service", bodyAsText())
        }
    }

    @Test
    fun `sync service saves new events which are sent`() = testApplication {
        var database : Database? = null
        application {
            configureSerialization()
            configureDatabase().let {
                database = it
                configureRouting(it)
            }
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val syncRequest = SyncRequest(setOf(
            CRDTDelta(
                "contacts",
                uuid4(),
                HybridLogicalClock(),
                "id",
                uuid4().toString()
            )
        ))

        client.post("/sync") {
            contentType(ContentType.Application.Json)
            setBody(syncRequest)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            assertTrue(database?.contactDeltaEventSourceQueries?.eventsSince(0)?.executeAsList()?.size == 1)
        }
    }

    @Test
    fun `sync service saves returns only events which after a given sync id`() = testApplication {
        var database : Database? = null
        application {
            configureSerialization()
            configureDatabase().let {
                database = it
                configureRouting(it)
            }
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val syncRequest = SyncRequest(setOf(
            CRDTDelta(
                "contacts",
                uuid4(),
                HybridLogicalClock(),
                "id",
                uuid4().toString()
            )
        ))

        val syncRequest2 = SyncRequest(setOf(
            CRDTDelta(
                "contacts",
                uuid4(),
                HybridLogicalClock(),
                "id",
                uuid4().toString()
            )
        ))

        client.post("/sync") {
            contentType(ContentType.Application.Json)
            setBody(syncRequest)
        }

        client.post("/sync") {
            contentType(ContentType.Application.Json)
            setBody(syncRequest2)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            assertTrue(database?.contactDeltaEventSourceQueries?.eventsSince(1)?.executeAsList()?.size == 1)
        }
    }
}
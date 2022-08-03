package com.contacts.crdtservice

import com.contacts.crdtservice.plugins.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureHTTP()
        configureMonitoring()
        configureSerialization()
        configureRouting(configureDatabase())
    }.start(wait = true)
}

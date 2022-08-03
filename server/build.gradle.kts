plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
}

val serviceNamespace = "com.contacts.crdtservice"

group = "com.example"
version = "0.0.1"

application {
    mainClass.set("$serviceNamespace.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

sqldelight {
    database("Database") {
        packageName = serviceNamespace
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += listOf("-Xcontext-receivers")
    }
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.content)
    implementation(libs.ktor.server.headers)
    implementation(libs.ktor.server.logging)
    implementation(libs.ktor.server.serialization)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.client.content)
    implementation(libs.logback)
    implementation(libs.sqldelight.driver)

    implementation(projects.common)

    testImplementation(libs.ktor.server.tests)
    testImplementation(libs.kotlin.junit)
}
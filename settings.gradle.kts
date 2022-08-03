pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven(url = "https://jitpack.io" )

    }
}

rootDir.listFiles()?.filter {
    File(it, "build.gradle.kts").exists()
}?.forEach {
    include(it.name)
}

rootProject.name = "crdt-demo"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
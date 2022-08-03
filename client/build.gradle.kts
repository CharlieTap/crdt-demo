@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.android)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.sqldelight)
}

android {

    namespace = libs.versions.application.namespace.get()
    compileSdk = libs.versions.compile.sdk.get().toInt()

    defaultConfig {

        applicationId = libs.versions.application.id.get()
        minSdk = libs.versions.min.sdk.get().toInt()
        targetSdk = libs.versions.target.sdk.get().toInt()
        versionName = libs.versions.version.name.get()

        testInstrumentationRunner =  "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {

        getByName("release") {
            isMinifyEnabled =  true
            isShrinkResources =  true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }

}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += listOf("-Xcontext-receivers")
    }
}

sqldelight {
    database("Database") {
        packageName = "com.tap.contacts"
    }
}

dependencies {

    implementation(projects.common)


    implementation(libs.bundles.androidx)
    implementation ("com.google.android.material:material:1.6.1")
    implementation(libs.bundles.compose.ui)
    implementation(libs.bundles.retrofit)
    implementation(libs.faker)
    implementation(libs.kotlinx.serialization)
    implementation(libs.sqldelight.android.driver)
    implementation(libs.sqldelight.coroutines)
    implementation(libs.result)

    implementation(libs.bundles.hilt.deps)
    kapt(libs.bundles.hilt.compilers)

    runtimeOnly(libs.kotlinx.coroutines.android)
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    id("com.android.application") version "8.4.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
    // Phase 5: Crashlytics. Applied conditionally in app/build.gradle.kts only when
    // google-services.json is present, so the build never hard-fails without Firebase setup.
    id("com.google.gms.google-services") version "4.4.2" apply false
    id("com.google.firebase.crashlytics") version "3.0.2" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

plugins {
    // Plugin per progetti Android multi-modulo
    id("com.android.application") version "8.2.2" apply false
    id("com.android.library") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.21" apply false
    id("org.jetbrains.kotlin.kapt") version "1.9.21" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}


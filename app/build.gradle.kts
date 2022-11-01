@file:Suppress("UnstableApiUsage")

import com.android.build.api.dsl.ApplicationBuildType
import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    id("com.android.application")
    id("androidx.navigation.safeargs")
}

val localProperties = gradleLocalProperties(rootDir)

android {
    signingConfigs {
        create("release") {
            val keyStoreFile = localProperties.getProperty("KEY_STORE_FILE")
            storeFile = if (keyStoreFile != null) file(keyStoreFile) else null
            storePassword = localProperties.getProperty("KEY_STORE_PASSWORD")
            keyAlias = localProperties.getProperty("KEY_ALIAS")
            keyPassword = localProperties.getProperty("KEY_PASSWORD")
        }
    }

    compileSdk = 32
    namespace = "com.jonahbauer.qed"

    defaultConfig {
        applicationId = "com.jonahbauer.qed"
        versionCode = 1
        versionName = "2.5.0-beta5"

        minSdk = 24
        targetSdk = 31
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        fun ApplicationBuildType.buildConfigFields(prerelease: Boolean) {
            buildConfigField("java.time.Instant", "TIMESTAMP", "java.time.Instant.ofEpochMilli(" + System.currentTimeMillis() + "L)")
            buildConfigField("boolean", "PRERELEASE", prerelease.toString())
            resValue("string", "preferences_general_update_check_includes_prerelease_default", prerelease.toString())
        }

        getByName("release") {
            postprocessing {
                isRemoveUnusedCode = true
                isObfuscate = false
                isOptimizeCode = true
            }

            signingConfig = signingConfigs.getByName("release")
            buildConfigFields(false)
        }

        getByName("debug") {
            isMinifyEnabled = false
            isDebuggable = true
            buildConfigFields(true)
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        targetCompatibility = JavaVersion.VERSION_11
        sourceCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
}


dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("androidx.appcompat:appcompat:1.5.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.fragment:fragment:1.5.4")
    implementation("androidx.navigation:navigation-fragment:2.5.3")
    implementation("androidx.navigation:navigation-ui:2.5.3")
    implementation("androidx.preference:preference:1.2.0")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.room:room-runtime:2.4.3")
    implementation("androidx.room:room-rxjava3:2.4.3")
    implementation("androidx.security:security-crypto:1.0.0")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("com.google.android.material:material:1.7.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.x5dev:chunk-templates:3.6.2")
    implementation("org.jsoup:jsoup:1.15.3")
    implementation("it.unimi.dsi:fastutil-core:8.5.8")
    implementation("io.reactivex.rxjava3:rxandroid:3.0.0")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.1.5")

    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")

    annotationProcessor("androidx.room:room-compiler:2.4.3")

    implementation("eu.jonahbauer:android-preference-annotations:1.1.2")
    annotationProcessor("eu.jonahbauer:android-preference-annotations:1.1.2")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:4.8.1")
    androidTestImplementation("androidx.test:core:1.4.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test:runner:1.4.0")
    androidTestImplementation("com.google.truth:truth:1.1.3")
}
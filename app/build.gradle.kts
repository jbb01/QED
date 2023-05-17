@file:Suppress("UnstableApiUsage")

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

    compileSdk = 33
    namespace = "eu.jonahbauer.qed"

    defaultConfig {
        applicationId = "eu.jonahbauer.qed"
        versionCode = 3
        versionName = "3.0.0-rc.1"

        minSdk = 24
        targetSdk = 31
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        all {
            buildConfigField(
                    "java.time.Instant", "BUILD_TIMESTAMP",
                    "java.time.Instant.ofEpochMilli(${System.currentTimeMillis()}L)"
            )
            buildConfigField(
                    "eu.jonahbauer.qed.model.Release", "RELEASE",
                    "new eu.jonahbauer.qed.model.Release(\"v" + defaultConfig.versionName + "\", BUILD_TIMESTAMP)"
            )

            val isPrerelease = defaultConfig.versionName!!.contains(Regex("^\\d+\\.\\d+\\.\\d+-"))
            buildConfigField("boolean", "PRERELEASE", isPrerelease.toString())
            resValue("string", "preferences_general_update_check_includes_prerelease_default", isPrerelease.toString())
        }

        getByName("release") {
            postprocessing {
                isRemoveUnusedCode = true
                isObfuscate = false
                isOptimizeCode = true
            }

            signingConfig = signingConfigs.getByName("release")
        }

        getByName("debug") {
            isMinifyEnabled = false
            isDebuggable = true
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
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.fragment:fragment:1.5.7")
    implementation("androidx.navigation:navigation-fragment:2.5.3")
    implementation("androidx.navigation:navigation-ui:2.5.3")
    implementation("androidx.preference:preference:1.2.0")
    implementation("androidx.recyclerview:recyclerview:1.3.0")
    implementation("androidx.room:room-runtime:2.5.1")
    implementation("androidx.room:room-rxjava3:2.5.1")
    implementation("androidx.security:security-crypto:1.0.0")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("com.google.android.material:material:1.8.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.x5dev:chunk-templates:3.6.2")
    implementation("org.jsoup:jsoup:1.15.4")
    implementation("it.unimi.dsi:fastutil-core:8.5.12")
    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")

    compileOnly("org.projectlombok:lombok:1.18.26")
    annotationProcessor("org.projectlombok:lombok:1.18.26")

    annotationProcessor("androidx.room:room-compiler:2.5.1")

    implementation("eu.jonahbauer:android-preference-annotations:1.1.2")
    annotationProcessor("eu.jonahbauer:android-preference-annotations:1.1.2")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:4.8.1")
    androidTestImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("com.google.truth:truth:1.1.3")
}
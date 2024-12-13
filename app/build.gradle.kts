@file:Suppress("UnstableApiUsage")

import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    id("com.android.application")
    id("androidx.navigation.safeargs")
}

val localProperties = gradleLocalProperties(rootDir, providers)

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

    compileSdk = 35
    namespace = "eu.jonahbauer.qed"

    defaultConfig {
        applicationId = "eu.jonahbauer.qed"
        versionCode = 6
        versionName = "3.1.0"

        minSdk = 24
        targetSdk = 35
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
            signingConfig = signingConfigs.getByName("release")

            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        buildConfig = true
    }
}


dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.fragment:fragment:1.8.5")
    implementation("androidx.navigation:navigation-fragment:2.8.4")
    implementation("androidx.navigation:navigation-ui:2.8.4")
    implementation("androidx.preference:preference:1.2.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.security:security-crypto:1.0.0")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.vectordrawable:vectordrawable-seekable:1.0.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.x5dev:chunk-templates:3.6.2")
    implementation("org.jsoup:jsoup:1.18.1")
    implementation("it.unimi.dsi:fastutil-core:8.5.13")
    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.3")

    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-rxjava3:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")

    implementation("eu.jonahbauer:android-preference-annotations:1.1.2")
    annotationProcessor("eu.jonahbauer:android-preference-annotations:1.1.2")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.11.0")

    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("com.google.truth:truth:1.4.2")
}

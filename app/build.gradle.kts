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

    compileSdk = 31

    defaultConfig {
        applicationId = "com.jonahbauer.qed"
        versionCode = 1
        versionName = "2.5.0-beta3"

        minSdk = 24
        targetSdk = 31
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
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
    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.3")
    implementation("androidx.fragment:fragment:1.4.1")
    implementation("androidx.navigation:navigation-fragment:2.4.1")
    implementation("androidx.navigation:navigation-ui:2.4.1")
    implementation("androidx.preference:preference:1.2.0")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.security:security-crypto:1.0.0")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("com.google.android.material:material:1.5.0")
    implementation("com.googlecode.json-simple:json-simple:1.1")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.x5dev:chunk-templates:3.6.1")
    implementation("org.apache.commons:commons-io:1.3.2")
    implementation("org.jsoup:jsoup:1.14.3")
    implementation("com.google.crypto.tink:tink-android:1.6.1")
    implementation("it.unimi.dsi:fastutil-core:8.5.6")
    implementation("androidx.room:room-runtime:2.4.2")
    implementation("androidx.room:room-rxjava3:2.4.2")
    implementation("io.reactivex.rxjava3:rxandroid:3.0.0")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.1.5")

    compileOnly("org.projectlombok:lombok:1.18.22")
    annotationProcessor("org.projectlombok:lombok:1.18.22")

    annotationProcessor("androidx.room:room-compiler:2.4.2")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:core:1.4.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test:runner:1.4.0")
    androidTestImplementation("com.google.truth:truth:1.1.3")
}
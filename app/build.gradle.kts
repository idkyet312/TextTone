@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.androidApplication)
    //id("com.android.application")
    //id("com.google.gms.google-services") version "4.4.1" apply false

}

apply(plugin = "com.google.gms.google-services")

android {
    namespace = "com.example.texttone"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.texttone"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    packagingOptions {
        exclude("META-INF/INDEX.LIST");
        exclude("META-INF/DEPENDENCIES");
        // Add more exclude lines here if needed
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildToolsVersion = "34.0.0"
}



dependencies {

    implementation("com.google.code.gson:gson:2.10.1")
    implementation(platform("com.google.firebase:firebase-bom:32.7.3"))

    implementation("com.google.firebase:firebase-analytics")

    // Google Cloud Speech-to-Text library
    implementation("com.google.cloud:google-cloud-speech:4.28.0")

    // You might also need to add the Google Cloud Storage client library if you're uploading files to Google Cloud Storage
    implementation("com.google.cloud:google-cloud-storage:2.29.1")
    // Google Auth Library
    implementation("com.google.auth:google-auth-library-oauth2-http:1.2.1")
    // OkHttp for network requests
    implementation("com.squareup.okhttp3:okhttp:4.9.1")

    // Kotlin Coroutines (if you're using Kotlin)
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.2")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)


}


// Top-level build file where you can add configuration options common to all sub-projects/modules.
@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.androidApplication) apply false
}
buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.4.1"); // Use the latest version available classpath 'com.google.gms:google-services:4.4.1'
    }
}


true // Needed to make the Suppress annotation work for the plugins block
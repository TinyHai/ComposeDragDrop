buildscript {
    val agp_version by extra("8.0.2")
}
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.3.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("com.android.library") version "8.3.0" apply false
    id("com.vanniktech.maven.publish") version "0.28.0" apply false
}
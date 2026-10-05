plugins {
    id("com.android.application")
    kotlin("android")
}
android {
    namespace = "ko.bura.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "ko.bura.app"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-prototype"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    lint { abortOnError = true }
}
dependencies { implementation(project(":core")) }

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    kotlin("kapt")
    id("kotlin-parcelize")
}

android {
    namespace = "com.shopix.buyer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.shopix.buyer"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.glide)
    kapt("com.github.bumptech.glide:compiler:4.16.0")
    implementation(libs.circleimageview)
    
    // ✅ Optimized Razorpay: Removed unnecessary Google Play Services
    implementation(libs.razorpay) {
        exclude(group = "com.google.android.gms", module = "play-services-auth")
        exclude(group = "com.google.android.gms", module = "play-services-auth-api-phone")
        exclude(group = "com.google.android.gms", module = "play-services-wallet")
    }
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)


    // ✅ Firebase BOM
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.squareup.okhttp3:okhttp:4.12.0")  // for profile photo upload
    
    // ✅ Optimized Firebase: Removed Google Sign-In / Credential Manager dependencies
    implementation("com.google.firebase:firebase-auth") {
        exclude(group = "com.google.android.gms", module = "play-services-auth")
        exclude(group = "androidx.credentials", module = "credentials-play-services-auth")
    }
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
}
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.watxaut.myjumpapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.watxaut.myjumpapp"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "com.watxaut.myjumpapp.HiltTestRunner"
        
        // Room schema export directory
        kapt {
            arguments {
                arg("room.schemaLocation", "$projectDir/schemas")
            }
        }
        
        // Support for 16KB page size devices
        ndk {
            debugSymbolLevel = "SYMBOL_TABLE"
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.camera.camera2.interop.ExperimentalCamera2Interop",
            "-opt-in=com.google.accompanist.permissions.ExperimentalPermissionsApi"
        )
    }
    
    buildFeatures {
        compose = true
        viewBinding = true // For camera integration
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = false
        }
    }
    
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {

    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose BOM and UI
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // ViewModel and Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Camera
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.extensions)

    // ML Kit for Computer Vision
    implementation(libs.mlkitObjectDetection)
    implementation(libs.mlkitPoseDetection)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // Charts for Data Visualization
    implementation(libs.mpandroidchart)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Dependency Injection - Hilt
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    kapt(libs.hilt.compiler)

    // Permissions
    implementation(libs.accompanist.permissions)

    // Unit Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.robolectric)
    testImplementation("app.cash.turbine:turbine:1.0.0") // Flow testing
    testImplementation("com.google.truth:truth:1.4.2") // Better assertions
    testImplementation("com.google.dagger:hilt-android-testing:2.48") // Hilt testing
    testImplementation("org.mockito:mockito-inline:5.2.0") // Inline mocking
    
    kaptTest("com.google.dagger:hilt-android-compiler:2.48") // Hilt compiler for tests

    // Android Instrumented Testing
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation("app.cash.turbine:turbine:1.0.0") // Flow testing
    androidTestImplementation("com.google.truth:truth:1.4.2") // Better assertions
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.48") // Hilt testing
    androidTestImplementation("androidx.test:core:1.5.0") // Test core utilities
    
    kaptAndroidTest("com.google.dagger:hilt-android-compiler:2.48") // Hilt compiler for tests

    // Debug Tools
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
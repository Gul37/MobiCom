plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.mobicom"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.mobicom"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {            // ← осы 3 жолды қосыңыз
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.navigation:navigation-fragment:2.7.7")   // ← осы 5 жолды
    implementation("androidx.navigation:navigation-ui:2.7.7")         //   қосыңыз
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata:2.8.7")
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
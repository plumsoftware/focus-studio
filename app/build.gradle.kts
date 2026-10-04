plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "ru.plumsoftware.focusstudio"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "ru.plumsoftware.focusstudio"
        minSdk = 24
        targetSdk = 37
        versionCode = 14
        versionName = "1.1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        renderscriptTargetApi = 23
        renderscriptSupportModeEnabled = true
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

    // Магазин приложений выбирается через product flavor.
    // PLATFORM: 1 = RuStore, 2 = Google Play, 3 = Huawei App Gallery.
    // Каждый флейвор задаёт свои боевые рекламные ID Yandex. В debug-сборке
    // (любой флейвор) AdsConfig подменяет их на демо-идентификаторы.
    flavorDimensions += "store"
    productFlavors {
        create("rustore") {
            dimension = "store"
            buildConfigField("int", "PLATFORM", "1")
            buildConfigField("String", "OPEN_ADS_ID", "\"R-M-19268030-1\"")
            buildConfigField("String", "INTERSTITIAL_ADS_ID", "\"R-M-19268030-2\"")
            buildConfigField("String", "NATIVE_ADS_ID", "\"R-M-19268030-3\"")
        }
        create("googleplay") {
            dimension = "store"
            buildConfigField("int", "PLATFORM", "2")
            buildConfigField("String", "OPEN_ADS_ID", "\"demo-appopenad-yandex\"")
            buildConfigField("String", "INTERSTITIAL_ADS_ID", "\"demo-interstitial-yandex\"")
            buildConfigField("String", "NATIVE_ADS_ID", "\"demo-native-content-yandex\"")
        }
        create("huawei") {
            dimension = "store"
            buildConfigField("int", "PLATFORM", "3")
            buildConfigField("String", "OPEN_ADS_ID", "\"R-M-19275668-1\"")
            buildConfigField("String", "INTERSTITIAL_ADS_ID", "\"R-M-19275668-2\"")
            buildConfigField("String", "NATIVE_ADS_ID", "\"R-M-19275668-3\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.9.8")

    // Extended icons
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    // Coil
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Exoplayer
    implementation("androidx.media3:media3-exoplayer:1.10.1")
    implementation("androidx.media3:media3-ui:1.10.1")
    implementation("androidx.media3:media3-transformer:1.10.1")
    implementation("androidx.media3:media3-effect:1.10.1")
    implementation("androidx.media3:media3-common:1.10.1")

    // Compose UI
    implementation(libs.androidx.ui.graphics)

    //Yandex Ads
    implementation("com.yandex.android:mobileads:8.5.0")
}
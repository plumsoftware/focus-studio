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
        versionCode = providers.gradleProperty("APP_VERSION_CODE").get().toInt()
        versionName = "1.1.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        renderscriptTargetApi = 23
        renderscriptSupportModeEnabled = true
    }

    signingConfigs {
        create("release") {
            // Keystore is committed at app/key_store; credentials are supplied by GitHub Secrets.
            storeFile = file("key_store")
            storePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").orNull ?: ""
            keyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").orNull ?: ""
            keyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull ?: ""
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
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
    // AdsConfig подменяет непустые ID на демо-идентификаторы; пустой ID
    // (Google Play) отключает соответствующую рекламу в любой сборке.
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
            // Для Google Play реклама пока не подключена: пустые ID отключают её полностью.
            buildConfigField("String", "OPEN_ADS_ID", "\"\"")
            buildConfigField("String", "INTERSTITIAL_ADS_ID", "\"\"")
            buildConfigField("String", "NATIVE_ADS_ID", "\"\"")
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
    implementation("com.yandex.android:mobileads:8.6.0")
}
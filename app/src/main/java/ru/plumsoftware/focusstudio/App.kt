package ru.plumsoftware.focusstudio

import android.app.Application
import com.yandex.mobile.ads.common.YandexAds

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        YandexAds.initialize(this) {}
    }
}
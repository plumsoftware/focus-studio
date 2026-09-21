package ru.plumsoftware.focusstudio.data

import ru.plumsoftware.focusstudio.BuildConfig

object AdsConfig {

    /**
     * Боевые рекламные ID задаются в product flavor (dimension "store")
     * в app/build.gradle.kts:
     *   - rustore    → PLATFORM 1 (RuStore)
     *   - googleplay → PLATFORM 2 (Google Play)
     *   - huawei     → PLATFORM 3 (Huawei App Gallery)
     *
     * В debug-сборке любого флейвора используются демо-идентификаторы Yandex,
     * в release — боевые ID выбранного магазина.
     */

    val OPEN_ADS_ID =
        if (BuildConfig.DEBUG) "demo-appopenad-yandex" else BuildConfig.OPEN_ADS_ID

    val INTERSTITIAL_ADS_ID =
        if (BuildConfig.DEBUG) "demo-interstitial-yandex" else BuildConfig.INTERSTITIAL_ADS_ID
}

package ru.plumsoftware.focusstudio.data

import ru.plumsoftware.focusstudio.BuildConfig

object AdsConfig {

    /**
     * Боевые рекламные ID задаются в product flavor (dimension "store")
     * в app/build.gradle.kts:
     *   - rustore    → PLATFORM 1 (RuStore)
     *   - googleplay → PLATFORM 2 (Google Play), ID пустые — рекламы нет
     *   - huawei     → PLATFORM 3 (Huawei App Gallery)
     *
     * В debug-сборке непустые ID заменяются демо-идентификаторами Yandex,
     * в release — боевые ID выбранного магазина. Пустой ID означает, что
     * этот вид рекламы отключён в любой сборке (см. [isEnabled]).
     */

    val OPEN_ADS_ID = resolve(BuildConfig.OPEN_ADS_ID, "demo-appopenad-yandex")

    val INTERSTITIAL_ADS_ID = resolve(BuildConfig.INTERSTITIAL_ADS_ID, "demo-interstitial-yandex")

    /** Нативная реклама под панелью инструментов фото- и видеоредактора. */
    val NATIVE_ADS_ID = resolve(BuildConfig.NATIVE_ADS_ID, "demo-native-content-yandex")

    /** Есть ли рекламный блок с таким ID — пустой ID загружать не нужно. */
    fun isEnabled(adUnitId: String): Boolean = adUnitId.isNotBlank()

    private fun resolve(flavorId: String, demoId: String): String = when {
        flavorId.isBlank() -> ""
        BuildConfig.DEBUG -> demoId
        else -> flavorId
    }
}

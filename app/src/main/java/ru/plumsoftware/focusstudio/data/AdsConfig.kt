package ru.plumsoftware.focusstudio.data

import ru.plumsoftware.focusstudio.BuildConfig

object AdsConfig {

    /**
    1 - RuStore
    2 - Google Play
    3 - Huawei App Gallery
     */

    val OPEN_ADS_ID = if (BuildConfig.DEBUG) "demo-appopenad-yandex" else if (BuildConfig.PLATFORM == 1) "R-M-19268030-1" else if (BuildConfig.PLATFORM == 3) "R-M-19275668-1" else "demo-appopenad-yandex"
}
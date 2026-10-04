package ru.plumsoftware.focusstudio.data

import android.content.Context

object AppPrefs {
    private const val PREFS_NAME = "focus_studio_prefs"
    private const val KEY_HAS_LAUNCHED_BEFORE = "has_launched_before"
    private const val KEY_SAVE_COUNT = "successful_save_count"

    /**
     * Сколько успешных сохранений (фото + видео суммарно) нужно сделать,
     * прежде чем включатся полноэкранные объявления: реклама при входе
     * и реклама после сохранения. Первые два сохранения — без рекламы,
     * третье и все последующие — с рекламой.
     */
    const val SAVES_BEFORE_FULLSCREEN_ADS = 3

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSaveCount(context: Context): Int = prefs(context).getInt(KEY_SAVE_COUNT, 0)

    /** Учитывает успешное сохранение и возвращает новое общее количество. */
    fun registerSuccessfulSave(context: Context): Int {
        val newCount = getSaveCount(context) + 1
        prefs(context).edit().putInt(KEY_SAVE_COUNT, newCount).apply()
        return newCount
    }

    /** Полноэкранная реклама разрешена, когда набрано нужное число сохранений. */
    fun isFullscreenAdsUnlocked(context: Context): Boolean =
        getSaveCount(context) >= SAVES_BEFORE_FULLSCREEN_ADS

    /**
     * Стоит ли заранее грузить межстраничную рекламу при открытии редактора:
     * да, если следующее сохранение уже будет с рекламой.
     */
    fun shouldPreloadSaveAd(context: Context): Boolean =
        getSaveCount(context) + 1 >= SAVES_BEFORE_FULLSCREEN_ADS

    /** Реклама при входе — только после [SAVES_BEFORE_FULLSCREEN_ADS] сохранений. */
    fun shouldShowLaunchAd(context: Context): Boolean = isFullscreenAdsUnlocked(context)

    fun markFirstLaunchComplete(context: Context) {
        prefs(context).edit().putBoolean(KEY_HAS_LAUNCHED_BEFORE, true).apply()
    }
}

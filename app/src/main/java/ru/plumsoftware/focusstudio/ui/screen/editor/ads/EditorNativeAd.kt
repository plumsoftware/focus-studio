package ru.plumsoftware.focusstudio.ui.screen.editor.ads

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import ru.plumsoftware.focusstudio.ui.theme.AccentStart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.yandex.mobile.ads.common.AdBindingResult
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.nativeads.MediaView
import com.yandex.mobile.ads.nativeads.NativeAd
import com.yandex.mobile.ads.nativeads.NativeAdLoadListener
import com.yandex.mobile.ads.nativeads.NativeAdLoader
import com.yandex.mobile.ads.nativeads.NativeAdView
import com.yandex.mobile.ads.nativeads.NativeAdViewBinder
import ru.plumsoftware.focusstudio.R
import ru.plumsoftware.focusstudio.data.AdsConfig

/**
 * Состояние нативной рекламы редактора. Живёт на уровне экрана редактора
 * (а не вкладки): объявление загружается и привязывается к View один раз
 * и остаётся тем же при переключении вкладок.
 */
@Stable
class EditorNativeAdState {
    var adView by mutableStateOf<NativeAdView?>(null)
        internal set

    /** Пользователь закрыл рекламу крестиком — до выхода из редактора она не вернётся. */
    var dismissed by mutableStateOf(false)
        internal set

    /** Прогресс ожидания перед тем, как крестик станет доступен (0..1). */
    internal val closeProgress = Animatable(0f)
}

/**
 * Создаёт состояние и ОДИН раз запускает загрузку нативной рекламы.
 * Вызывать на верхнем уровне экрана редактора.
 */
@Composable
fun rememberEditorNativeAdState(
    adUnitId: String = AdsConfig.NATIVE_ADS_ID
): EditorNativeAdState {
    val context = LocalContext.current
    val state = remember { EditorNativeAdState() }

    DisposableEffect(adUnitId) {
        val loader = NativeAdLoader(context)
        loader.loadAd(
            AdRequest.Builder(adUnitId).build(),
            object : NativeAdLoadListener {
                override fun onAdLoaded(nativeAd: NativeAd) {
                    state.adView = inflateAndBindNativeAd(context, nativeAd)
                }

                override fun onAdFailedToLoad(error: AdRequestError) {
                    // Рекламы нет — блок не показывается и места не занимает.
                    state.adView = null
                }
            }
        )
        onDispose { loader.cancelLoading() }
    }
    return state
}

/** Максимальная высота рекламного блока. */
private val AD_MAX_HEIGHT = 70.dp

/** Сколько ждать, прежде чем крестик станет доступен. */
private const val CLOSE_DELAY_MS = 5_000

/**
 * Блок нативной рекламы. Ставится в самый низ панели инструментов, ВНЕ
 * `when (activeTool)`: остальной контент сдвигается вверх, а блок при смене
 * вкладок не пересоздаётся. Пока объявления нет — высота нулевая.
 *
 * Справа — крестик: 5 секунд вокруг него заполняется кольцо, после этого
 * по нажатию реклама исчезает, а контейнер плавно схлопывается.
 */
@Composable
fun EditorNativeAd(state: EditorNativeAdState, modifier: Modifier = Modifier) {
    val adView = state.adView

    // Отсчёт идёт с момента появления объявления и не сбрасывается при смене вкладок.
    LaunchedEffect(adView) {
        if (adView != null && state.closeProgress.value < 1f) {
            val remaining = ((1f - state.closeProgress.value) * CLOSE_DELAY_MS).toInt()
            state.closeProgress.animateTo(1f, tween(remaining, easing = LinearEasing))
        }
    }

    AnimatedVisibility(
        visible = adView != null && !state.dismissed,
        enter = fadeIn(tween(250)) + expandVertically(tween(250)),
        exit = fadeOut(tween(200)) + shrinkVertically(tween(250)),
        modifier = modifier
    ) {
        if (adView == null) return@AnimatedVisibility

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = AD_MAX_HEIGHT)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = {
                    (adView.parent as? ViewGroup)?.removeView(adView)
                    adView
                }
            )

            val progress = state.closeProgress.value
            NativeAdCloseButton(
                progress = progress,
                enabled = progress >= 1f,
                onClose = { state.dismissed = true },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    // 12dp — отступ карточки от края, 7dp — её внутренний отступ;
                    // bottom = 4dp поднимает крестик на 2dp — ровно по центру карточки
                    .padding(end = 19.dp, bottom = 4.dp)
            )
        }
    }
}

/** Небольшой крестик в кружке с кольцом-таймером по окружности. */
@Composable
private fun NativeAdCloseButton(
    progress: Float,
    enabled: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.14f))
            .clickable(enabled = enabled, onClick = onClose),
        contentAlignment = Alignment.Center
    ) {
        if (!enabled) {
            Canvas(Modifier.size(24.dp)) {
                val stroke = 2.dp.toPx()
                val inset = stroke / 2f
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(
                    color = Color.White.copy(alpha = 0.12f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(stroke)
                )
                drawArc(
                    color = AccentStart,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
        }
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = null,
            tint = Color.White.copy(alpha = if (enabled) 0.9f else 0.35f),
            modifier = Modifier.size(12.dp)
        )
    }
}

/** Надувает res/layout/view_native_ad_editor.xml и привязывает к нему объявление. */
private fun inflateAndBindNativeAd(context: Context, nativeAd: NativeAd): NativeAdView? {
    val nativeAdView = LayoutInflater.from(context)
        .inflate(R.layout.view_native_ad_editor, null) as NativeAdView

    val media = nativeAdView.findViewById<MediaView>(R.id.native_ad_media)
    val icon = nativeAdView.findViewById<ImageView>(R.id.native_ad_icon)
    val favicon = nativeAdView.findViewById<ImageView>(R.id.native_ad_favicon)
    val title = nativeAdView.findViewById<TextView>(R.id.native_ad_title)
    val body = nativeAdView.findViewById<TextView>(R.id.native_ad_body)
    val callToAction = nativeAdView.findViewById<TextView>(R.id.native_ad_call_to_action)
    val warning = nativeAdView.findViewById<TextView>(R.id.native_ad_warning)
    val domain = nativeAdView.findViewById<TextView>(R.id.native_ad_domain)
    val sponsored = nativeAdView.findViewById<TextView>(R.id.native_ad_sponsored)
    val feedback = nativeAdView.findViewById<ImageView>(R.id.native_ad_feedback)
    val age = nativeAdView.findViewById<TextView>(R.id.native_ad_age)
    val price = nativeAdView.findViewById<TextView>(R.id.native_ad_price)

    val binder = NativeAdViewBinder.Builder(nativeAdView)
        .setMediaView(media)
        .setIconView(icon)
        .setFaviconView(favicon)
        .setTitleView(title)
        .setBodyView(body)
        .setCallToActionView(callToAction)
        .setWarningView(warning)
        .setDomainView(domain)
        .setSponsoredView(sponsored)
        .setFeedbackView(feedback)
        .setAgeView(age)
        .setPriceView(price)
        .build()

    if (nativeAd.bindNativeAd(binder) !is AdBindingResult.Success) return null

    // В квадратном слоте показываем что-то одно: иконку приложения, а если её нет — медиа.
    if (icon.drawable != null) {
        media.visibility = View.GONE
    } else {
        icon.visibility = View.GONE
        media.visibility = View.VISIBLE
        media.clipToOutline = true
    }

    // Пустые компоненты не должны занимать место.
    listOf(body, warning, price, age, domain, callToAction).forEach { view ->
        view.visibility = if (view.text.isNullOrBlank()) View.GONE else View.VISIBLE
    }
    // Высота блока ограничена 70dp: при наличии предупреждения оно идёт вместо основного текста.
    if (warning.visibility == View.VISIBLE) body.visibility = View.GONE
    favicon.visibility = if (favicon.drawable != null) View.VISIBLE else View.GONE

    return nativeAdView
}
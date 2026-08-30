package dev.hloth.zaragoza_tarjeta_bus

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.LocaleList
import android.text.TextUtils
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.hloth.zaragoza_tarjeta_bus.ui.MainScreen
import dev.hloth.zaragoza_tarjeta_bus.ui.sampleAvanzaTopUpCard
import dev.hloth.zaragoza_tarjeta_bus.ui.theme.ZaragozaTarjetaBusTheme
import java.io.File
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private val STORE_LOCALES = listOf(
    "en-US", "es-ES", "fr-FR", "de-DE", "ar", "ru-RU", "uk", "be", "ca", "ro",
)

private data class Shot(val locale: String, val index: Int, val filled: Boolean)

@Composable
private fun Localized(locale: Locale, content: @Composable () -> Unit) {
    val base = LocalContext.current
    val localized = remember(locale) { base.localizedTo(locale) }
    CompositionLocalProvider(
        LocalContext provides localized,
        LocalConfiguration provides localized.resources.configuration,
        LocalLayoutDirection provides locale.layoutDirection(),
        content = content,
    )
}

private fun Context.localizedTo(locale: Locale): Context =
    createConfigurationContext(
        Configuration(resources.configuration).apply {
            setLocales(LocaleList(locale))
            setLayoutDirection(locale)
        },
    )

private fun Locale.layoutDirection(): LayoutDirection =
    if (TextUtils.getLayoutDirectionFromLocale(this) == View.LAYOUT_DIRECTION_RTL) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }

@RunWith(AndroidJUnit4::class)
class StoreScreenshots {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun captureEveryStoreLocale() {
        val outputDir = (
            InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")?.let(::File)
                ?: File(
                    InstrumentationRegistry.getInstrumentation().targetContext
                        .getExternalFilesDir(null),
                    "screenshots",
                )
            ).apply { mkdirs() }

        val shots = STORE_LOCALES.flatMap {
            listOf(Shot(it, 1, filled = false), Shot(it, 2, filled = true))
        }
        var shot by mutableStateOf(shots.first())

        compose.setContent {
            Localized(Locale.forLanguageTag(shot.locale)) {
                ZaragozaTarjetaBusTheme(darkTheme = false, dynamicColor = false) {
                    if (shot.filled) MainScreen(card = sampleAvanzaTopUpCard) else MainScreen()
                }
            }
        }

        val systemLocale = Locale.getDefault()
        shots.forEach { next ->
            Locale.setDefault(Locale.forLanguageTag(next.locale))
            shot = next
            compose.waitForIdle()
            val bitmap = compose.onRoot().captureRetrying().asAndroidBitmap()
            File(outputDir, "${next.locale}-${next.index}.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
        Locale.setDefault(systemLocale)
    }
}

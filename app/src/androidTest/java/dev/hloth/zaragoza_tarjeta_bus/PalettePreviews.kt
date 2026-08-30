package dev.hloth.zaragoza_tarjeta_bus

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.hloth.zaragoza_tarjeta_bus.ui.MainScreen
import dev.hloth.zaragoza_tarjeta_bus.ui.sampleAvanzaTopUpCard
import dev.hloth.zaragoza_tarjeta_bus.ui.theme.Typography
import java.io.File
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PalettePreviews {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun captureEveryCandidatePalette() {
        val outputDir = (
            InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")?.let(::File)
                ?: File(
                    InstrumentationRegistry.getInstrumentation().targetContext
                        .getExternalFilesDir(null),
                    "palettes",
                )
            ).apply { mkdirs() }

        Locale.setDefault(Locale.forLanguageTag("en-US"))
        var candidate by mutableStateOf(CANDIDATE_PALETTES.first())

        compose.setContent {
            MaterialTheme(colorScheme = candidate.scheme, typography = Typography) {
                MainScreen(card = sampleAvanzaTopUpCard)
            }
        }

        CANDIDATE_PALETTES.forEach { next ->
            candidate = next
            compose.waitForIdle()
            val bitmap = compose.onRoot().captureRetrying().asAndroidBitmap()
            File(outputDir, "${next.name}.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }
}

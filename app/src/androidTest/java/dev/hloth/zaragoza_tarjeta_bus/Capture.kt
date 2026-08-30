package dev.hloth.zaragoza_tarjeta_bus

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage

internal fun SemanticsNodeInteraction.captureRetrying(): ImageBitmap {
    var failure: AssertionError? = null
    repeat(4) {
        try {
            return captureToImage()
        } catch (error: AssertionError) {
            failure = error
            Thread.sleep(300)
        }
    }
    throw checkNotNull(failure)
}

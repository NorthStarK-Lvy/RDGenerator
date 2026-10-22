package com.rdgen.app

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** 窄屏（360dp 宽）下再画一张，确认输入框、胶囊、网格都不会挤爆。 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h780dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotNarrowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun captureNarrowResults() {
        composeRule.onNodeWithText("生成随机数").performClick()
        composeRule.waitForIdle()
        Thread.sleep(700)
        composeRule.waitForIdle()

        val decor = composeRule.activity.window.decorView
        val width = decor.width.coerceAtLeast(720)
        val height = decor.height.coerceAtLeast(1560)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        decor.draw(Canvas(bitmap))

        val dir = File("build/screenshots").apply { mkdirs() }
        File(dir, "08-narrow.png").outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}

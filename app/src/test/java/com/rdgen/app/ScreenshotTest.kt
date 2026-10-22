package com.rdgen.app

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * 只为「没有真机时也能看一眼界面」服务：用 Robolectric 在 JVM 上把 Activity 画成 PNG。
 * 产物在 app/build/screenshots/，不参与 APK。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun captureHome() {
        settle()
        capture("01-home.png")
    }

    @Test
    fun captureResults() {
        composeRule.onNodeWithText("生成随机数").performClick()
        settle(700)
        capture("02-results.png")
    }

    @Test
    fun captureManyResults() {
        setRange("1", "1000")
        // 个数拉到 100：点一次 + 只是 +1，这里直接验证 12 个的多行网格
        repeat(7) { composeRule.onNodeWithContentDescription("增加一个").performClick() }
        composeRule.onNodeWithText("不重复").performClick()
        composeRule.onNodeWithText("升序排列").performClick()
        composeRule.onNodeWithText("生成随机数").performClick()
        settle(900)
        capture("03-many.png")
    }

    @Test
    fun captureInvalidDistinct() {
        setRange("1", "5")
        repeat(5) { composeRule.onNodeWithContentDescription("增加一个").performClick() }
        composeRule.onNodeWithText("不重复").performClick()
        settle()
        capture("04-warning.png")
    }

    @Test
    fun captureHistory() {
        composeRule.onNodeWithText("生成随机数").performClick()
        settle(500)
        composeRule.onNodeWithContentDescription("历史记录").performClick()
        // ModalBottomSheet 有进场动画，Robolectric 下多等一会儿再画
        settle(1200)
        capture("05-history.png")
    }

    @Test
    fun captureInvalidRange() {
        setRange("50", "10")
        settle()
        capture("07-invalid-range.png")
    }

    private fun setRange(min: String, max: String) {
        val fields = composeRule.onAllNodes(hasSetTextAction())
        fields[0].performTextReplacement(min)
        fields[1].performTextReplacement(max)
        settle()
    }

    private fun settle(millis: Long = 300) {
        composeRule.waitForIdle()
        if (millis > 0) {
            Thread.sleep(millis)
            composeRule.waitForIdle()
        }
    }

    private fun capture(name: String) {
        val activity = composeRule.activity
        val decor = activity.window.decorView
        val width = decor.width.coerceAtLeast(1080)
        val height = decor.height.coerceAtLeast(2340)
        if (decor.width == 0 || decor.height == 0) {
            decor.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(height, android.view.View.MeasureSpec.EXACTLY),
            )
            decor.layout(0, 0, width, height)
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        decor.draw(Canvas(bitmap))

        val dir = File("build/screenshots").apply { mkdirs() }
        File(dir, name).outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        println("screenshot -> ${File(dir, name).absolutePath} (${width}x$height)")
    }
}

/** 深色模式单独跑一份，检查对比度与配色。 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-night-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotDarkTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun captureDarkResults() {
        composeRule.onNodeWithText("生成随机数").performClick()
        composeRule.waitForIdle()
        Thread.sleep(700)
        composeRule.waitForIdle()

        val decor = composeRule.activity.window.decorView
        val width = decor.width.coerceAtLeast(1080)
        val height = decor.height.coerceAtLeast(2340)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        decor.draw(Canvas(bitmap))

        val dir = File("build/screenshots").apply { mkdirs() }
        File(dir, "06-dark.png").outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}

package com.rdgen.app

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 界面行为测试（JVM + Robolectric）：
 * 不测像素，只钉住「按钮能不能点、结果出没出、历史能不能恢复、复制内容对不对」。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GeneratorUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun settle() {
        composeRule.waitForIdle()
        Thread.sleep(250)
        composeRule.waitForIdle()
    }

    private fun setRange(min: String, max: String) {
        val fields = composeRule.onAllNodes(hasSetTextAction())
        fields[0].performTextReplacement(min)
        fields[1].performTextReplacement(max)
        settle()
    }

    @Test
    fun generateButtonProducesResults() {
        composeRule.onNodeWithText("生成随机数").assertIsEnabled()
        composeRule.onNodeWithText("生成随机数").performClick()
        settle()

        composeRule.onNodeWithText("共 5 个 · 1–100").assertExists()
        // 5 个结果卡片，每个都是纯数字
        var found = 0
        composeRule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes()
        repeat(100) { value ->
            val node = composeRule.onAllNodes(
                androidx.compose.ui.test.hasText(value.toString(), substring = false)
            ).fetchSemanticsNodes()
            found += node.size
        }
        assertTrue("expected some numeric result tiles", found >= 1)
    }

    @Test
    fun reversedRangeDisablesGenerate() {
        setRange("50", "10")
        composeRule.onNodeWithText("生成随机数").assertIsNotEnabled()
        composeRule.onNodeWithText("不能小于最小值").assertExists()
    }

    @Test
    fun impossibleDistinctShowsWarning() {
        setRange("1", "5")
        repeat(6) { composeRule.onNodeWithContentDescription("增加一个").performClick() }
        composeRule.onNodeWithText("不重复").performClick()
        settle()

        composeRule.onNodeWithText("该范围内只有 5 个整数，取不了 11 个不重复的数").assertExists()
        composeRule.onNodeWithText("生成随机数").assertIsNotEnabled()
    }

    @Test
    fun copyPutsNumbersOnClipboard() {
        composeRule.onNodeWithText("生成随机数").performClick()
        settle()
        composeRule.onNodeWithContentDescription("复制全部").performClick()
        settle()

        val clipboard = composeRule.activity
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
        assertNotNull(text)
        val parts = text!!.split(", ")
        assertEquals(5, parts.size)
        parts.forEach { part ->
            val value = part.toLongOrNull()
            assertNotNull("not a number: $part", value)
            assertTrue("out of range: $value", value!! in 1L..100L)
        }
    }

    @Test
    fun historySheetListsAndRestoresEntry() {
        setRange("1", "10")
        composeRule.onNodeWithText("生成随机数").performClick()
        settle()

        composeRule.onNodeWithContentDescription("历史记录").performClick()
        settle()
        composeRule.onNodeWithText("最近 1 次，点一条可恢复").assertExists()

        // 点这条记录 → 面板关闭、范围恢复成 1–10
        // （历史行的合并文案形如「1–10,  · 5 个, 10-18 09:20, ...」，用「个」定位，
        //   否则会和范围预设里的「1–10」胶囊撞名）
        composeRule.onNodeWithText(" · 5 个", substring = true).performClick()
        settle()
        composeRule.onNodeWithText("共 5 个 · 1–10").assertExists()
    }

    @Test
    fun clearRemovesResults() {
        composeRule.onNodeWithText("生成随机数").performClick()
        settle()
        composeRule.onNodeWithContentDescription("清空结果").performClick()
        settle()
        composeRule.onNodeWithText("还没有生成").assertExists()
    }
}

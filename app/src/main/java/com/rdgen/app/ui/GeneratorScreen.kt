package com.rdgen.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rdgen.app.core.RandomEngine
import com.rdgen.app.data.HistoryEntry
import com.rdgen.app.data.HistoryStore
import com.rdgen.app.ui.components.AppIcons
import com.rdgen.app.ui.components.DiceArt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** 结果区标题在网格里的位置（前面固定有 4 个整宽条目）。 */
private const val RESULTS_HEADER_INDEX = 4

private val RangePresets = listOf(
    1L to 6L,
    1L to 10L,
    1L to 100L,
    1L to 1000L,
)

@Composable
fun GeneratorScreen() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val gridState = rememberLazyGridState()
    val historyStore = remember(context) { HistoryStore(context) }

    var minText by rememberSaveable { mutableStateOf("1") }
    var maxText by rememberSaveable { mutableStateOf("100") }
    var count by rememberSaveable { mutableIntStateOf(5) }
    var distinct by rememberSaveable { mutableStateOf(false) }
    var ascending by rememberSaveable { mutableStateOf(false) }
    var results by rememberSaveable { mutableStateOf(longArrayOf()) }
    var resultRange by rememberSaveable { mutableStateOf("") }
    var generation by rememberSaveable { mutableIntStateOf(0) }
    var history by remember { mutableStateOf(historyStore.load()) }
    var historyOpen by rememberSaveable { mutableStateOf(false) }

    val minValue = minText.toLongOrNull()
    val maxValue = maxText.toLongOrNull()

    val minError = when {
        minText.isBlank() -> "请输入最小值"
        minValue == null -> "请输入整数"
        minValue < 1L -> "只能填正整数"
        else -> null
    }
    val maxError = when {
        maxText.isBlank() -> "请输入最大值"
        maxValue == null -> "请输入整数"
        maxValue < 1L -> "只能填正整数"
        minValue != null && maxValue < minValue -> "不能小于最小值"
        else -> null
    }
    val span = if (minValue != null && maxValue != null && maxValue >= minValue) {
        maxValue - minValue + 1L
    } else {
        0L
    }
    val notEnoughDistinct = distinct && span in 1L until count.toLong()
    val canGenerate = minError == null && maxError == null && !notEnoughDistinct

    fun generate() {
        val min = minText.toLongOrNull() ?: return
        val max = maxText.toLongOrNull() ?: return
        when (val outcome = RandomEngine.generate(min, max, count, distinct, ascending)) {
            is RandomEngine.Outcome.Invalid -> scope.launch {
                snackbarHostState.showSnackbar(outcome.message)
            }

            is RandomEngine.Outcome.Success -> {
                focusManager.clearFocus()
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                results = outcome.values.toLongArray()
                resultRange = "$min–$max"
                generation += 1

                val entry = HistoryEntry(
                    timestamp = System.currentTimeMillis(),
                    min = min,
                    max = max,
                    count = count,
                    distinct = distinct,
                    ascending = ascending,
                    values = outcome.values,
                )
                historyStore.add(entry)
                history = (listOf(entry) + history).take(HistoryStore.MAX_ENTRIES)

                scope.launch { gridState.animateScrollToItem(RESULTS_HEADER_INDEX) }
            }
        }
    }

    fun restore(entry: HistoryEntry) {
        minText = entry.min.toString()
        maxText = entry.max.toString()
        count = entry.count.coerceIn(1, RandomEngine.MAX_COUNT)
        distinct = entry.distinct
        ascending = entry.ascending
        results = entry.values.toLongArray()
        resultRange = "${entry.min}–${entry.max}"
        generation += 1
        scope.launch { gridState.animateScrollToItem(RESULTS_HEADER_INDEX) }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
        ),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            GenerateBar(enabled = canGenerate, onClick = ::generate)
        },
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 96.dp),
            state = gridState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ScreenHeader(onHistoryClick = { historyOpen = true })
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                RangeCard(
                    minText = minText,
                    maxText = maxText,
                    minError = minError,
                    maxError = maxError,
                    span = span,
                    onMinChange = { minText = it.filter(Char::isDigit).take(9) },
                    onMaxChange = { maxText = it.filter(Char::isDigit).take(9) },
                    onPreset = { min, max ->
                        minText = min.toString()
                        maxText = max.toString()
                    },
                    onDone = { focusManager.clearFocus() },
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                CountCard(
                    count = count,
                    onCountChange = { count = it.coerceIn(1, RandomEngine.MAX_COUNT) },
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                OptionsCard(
                    distinct = distinct,
                    ascending = ascending,
                    onDistinctChange = { distinct = it },
                    onAscendingChange = { ascending = it },
                    warning = if (notEnoughDistinct) {
                        "该范围内只有 $span 个整数，取不了 $count 个不重复的数"
                    } else {
                        null
                    },
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                ResultsHeader(
                    results = results,
                    rangeLabel = resultRange.ifEmpty { null },
                    onCopy = {
                        copyToClipboard(context, results.joinToString(", "))
                        scope.launch { snackbarHostState.showSnackbar("已复制 ${results.size} 个数字") }
                    },
                    onClear = {
                        results = longArrayOf()
                        resultRange = ""
                        generation += 1
                    },
                )
            }

            if (results.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState()
                }
            } else {
                items(
                    count = results.size,
                    key = { index -> "gen-$generation-$index" },
                ) { index ->
                    NumberTile(
                        value = results[index],
                        index = index,
                        generation = generation,
                    )
                }
            }
        }
    }

    if (historyOpen) {
        HistorySheet(
            entries = history,
            onDismiss = { historyOpen = false },
            onClear = {
                historyStore.clear()
                history = emptyList()
                scope.launch { snackbarHostState.showSnackbar("历史记录已清空") }
            },
            onSelect = { entry ->
                restore(entry)
                historyOpen = false
            },
        )
    }
}

@Composable
private fun ScreenHeader(onHistoryClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "随机数生成器",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "正整数 · 范围与个数自由调",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FilledTonalIconButton(onClick = onHistoryClick) {
            Icon(AppIcons.History, contentDescription = "历史记录")
        }
    }
}

@Composable
private fun RangeCard(
    minText: String,
    maxText: String,
    minError: String?,
    maxError: String?,
    span: Long,
    onMinChange: (String) -> Unit,
    onMaxChange: (String) -> Unit,
    onPreset: (Long, Long) -> Unit,
    onDone: () -> Unit,
) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "取值范围",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = if (span > 0) "共 $span 个整数" else "—",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RangeField(
                value = minText,
                onValueChange = onMinChange,
                label = "最小值",
                isError = minError != null,
                modifier = Modifier.weight(1f),
                onDone = onDone,
            )
            RangeField(
                value = maxText,
                onValueChange = onMaxChange,
                label = "最大值",
                isError = maxError != null,
                modifier = Modifier.weight(1f),
                onDone = onDone,
            )
        }

        // 错误提示单独占一行：放进各自的 supportingText 会让两个输入框高度不齐。
        val errorText = minError ?: maxError
        if (errorText != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RangePresets.forEach { (min, max) ->
                val selected = minText == min.toString() && maxText == max.toString()
                FilterChip(
                    selected = selected,
                    onClick = { onPreset(min, max) },
                    label = { Text("$min–$max") },
                    leadingIcon = if (selected) {
                        {
                            Icon(
                                AppIcons.Check,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                            )
                        }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
private fun RangeField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
    onDone: () -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
    )
}

@Composable
private fun CountCard(
    count: Int,
    onCountChange: (Int) -> Unit,
) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "生成个数",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "一次最多 ${RandomEngine.MAX_COUNT} 个",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Text(
                    text = "$count",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(
                onClick = { onCountChange(count - 1) },
                enabled = count > 1,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(AppIcons.Remove, contentDescription = "减少一个")
            }

            Slider(
                value = count.toFloat(),
                onValueChange = { onCountChange(it.roundToInt()) },
                valueRange = 1f..RandomEngine.MAX_COUNT.toFloat(),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            )

            FilledTonalIconButton(
                onClick = { onCountChange(count + 1) },
                enabled = count < RandomEngine.MAX_COUNT,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(AppIcons.Add, contentDescription = "增加一个")
            }
        }
    }
}

@Composable
private fun OptionsCard(
    distinct: Boolean,
    ascending: Boolean,
    onDistinctChange: (Boolean) -> Unit,
    onAscendingChange: (Boolean) -> Unit,
    warning: String?,
) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "生成方式",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = if (distinct) "每个数只出现一次" else "允许重复",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OptionChip(
                label = "不重复",
                selected = distinct,
                onClick = { onDistinctChange(!distinct) },
            )
            OptionChip(
                label = "升序排列",
                selected = ascending,
                onClick = { onAscendingChange(!ascending) },
            )
        }

        if (warning != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = warning,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun OptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            {
                Icon(
                    AppIcons.Check,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            }
        } else {
            null
        },
    )
}

@Composable
private fun ResultsHeader(
    results: LongArray,
    rangeLabel: String?,
    onCopy: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "生成结果",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (results.isEmpty()) {
                    "还没有生成"
                } else {
                    buildString {
                        append("共 ${results.size} 个")
                        if (rangeLabel != null) append(" · $rangeLabel")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (results.isNotEmpty()) {
            IconButton(onClick = onCopy) {
                Icon(AppIcons.ContentCopy, contentDescription = "复制全部")
            }
            IconButton(onClick = onClear) {
                Icon(AppIcons.Delete, contentDescription = "清空结果")
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DiceArt(size = 64.dp)
            Spacer(Modifier.width(18.dp))
            Column {
                Text(
                    text = "调好范围，点下方按钮",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "结果会在这里逐个蹦出来",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NumberTile(
    value: Long,
    index: Int,
    generation: Int,
) {
    var visible by remember(generation) { mutableStateOf(false) }

    LaunchedEffect(generation) {
        delay((index.coerceAtMost(24) * 22).toLong())
        visible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.82f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "tileScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 160),
        label = "tileAlpha",
    )

    val digits = value.toString().length
    val fontSize = when {
        digits <= 3 -> 30.sp
        digits <= 5 -> 23.sp
        digits <= 7 -> 18.sp
        else -> 15.sp
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = value.toString(),
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 10.dp),
            )
        }
    }
}

@Composable
private fun GenerateBar(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Button(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .height(56.dp),
                shape = CircleShape,
            ) {
                Icon(
                    imageVector = AppIcons.Casino,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "生成随机数",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            content = content,
        )
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("随机数", text))
}

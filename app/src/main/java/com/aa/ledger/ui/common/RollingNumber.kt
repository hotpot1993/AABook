package com.aa.ledger.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import com.aa.ledger.ui.theme.AnimSpec
import kotlinx.coroutines.delay

/**
 * 数字垂直滚动（odometer / NumberFlow 风格）。
 *
 * [value] 每次变化（首次加载、实时刷新）都触发逐位滚动；
 * 首次进入（无历史值）或值未变时静态显示。
 * 滚动方向自动判断：金额增大向上滚、减小向下滚。
 */
@Composable
fun RollingNumber(
    value: Double,
    format: (Double) -> String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current
) {
    // 跨导航记忆「上次展示值」，用于判断变化。
    var lastValue by rememberSaveable { mutableStateOf(-1.0) }

    // 捕获滚动起点：value 变化的那一刻 lastValue 仍是旧值；remember(value) 保证滚动期间稳定。
    val fromValue = remember(value) { lastValue }
    val fromText = format(if (fromValue >= 0.0) fromValue else value)
    val toText = format(value)
    val shouldRoll = fromValue >= 0.0 && fromValue != value && fromText.length == toText.length
    val rollUp = value >= fromValue

    // 展示后同步 lastValue（不影响本次滚动，因 fromValue 已随 value 固化）。
    LaunchedEffect(value) { lastValue = value }

    if (shouldRoll) {
        RollingText(fromText, toText, rollUp, style, modifier)
    } else {
        Text(toText, style = style, modifier = modifier)
    }
}

@Composable
private fun RollingText(
    from: String,
    to: String,
    rollUp: Boolean,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        from.forEachIndexed { i, oldChar ->
            val newChar = to.getOrNull(i) ?: return@forEachIndexed
            if (oldChar.isDigit() && newChar.isDigit() && oldChar != newChar) {
                DigitWheel(
                    from = oldChar.digitToInt(),
                    to = newChar.digitToInt(),
                    rollUp = rollUp,
                    delayMillis = i * AnimSpec.StaggerInterval,
                    style = style
                )
            } else {
                Text(newChar.toString(), style = style)
            }
        }
    }
}

@Composable
private fun DigitWheel(
    from: Int,
    to: Int,
    rollUp: Boolean,
    delayMillis: Int,
    style: TextStyle
) {
    val density = LocalDensity.current
    val glyphHeightPx = with(density) { style.fontSize.toPx() * 1.2f }
    val glyphHeightDp = with(density) { glyphHeightPx.toDp() }

    // 三段 "0123456789" 拼成 30 位字符条，中间一段（索引 10..19）为基准，
    // 上下各留一段用于处理进位（9→0 向上滚、0→9 向下滚）。
    val strip = "012345678901234567890123456789"
    val center = 10
    val start = center + from
    val target = if (rollUp) {
        if (to >= from) center + to else center + 10 + to
    } else {
        if (to <= from) center + to else center - 10 + to
    }

    val anim = remember { Animatable(start.toFloat()) }
    LaunchedEffect(to, rollUp) {
        delay(delayMillis.toLong())
        anim.animateTo(
            target.toFloat(),
            animationSpec = tween(durationMillis = AnimSpec.TransitionDuration, easing = AnimSpec.IosEaseOut)
        )
    }

    Box(
        modifier = Modifier
            .height(glyphHeightDp)
            .clipToBounds()
    ) {
        Column(
            modifier = Modifier.graphicsLayer { translationY = -anim.value * glyphHeightPx }
        ) {
            strip.forEach { c ->
                Box(
                    modifier = Modifier.height(glyphHeightDp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(c.toString(), style = style)
                }
            }
        }
    }
}

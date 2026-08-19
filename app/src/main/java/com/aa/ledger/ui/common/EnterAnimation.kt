package com.aa.ledger.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.aa.ledger.ui.theme.AnimSpec
import kotlinx.coroutines.delay

/**
 * 多米诺 stagger 入场：按 [index] 依次「上滑 + 淡入」，相邻项间隔 [AnimSpec.StaggerInterval]。
 *
 * 用 graphicsLayer 只做位移 + 透明度，不改变布局尺寸，避免列表项入场时整体重排抖动。
 * 用于首页账本卡片、流水行、统计卡片等列表内容，保证全应用入场节奏一致。
 */
@Composable
fun Modifier.staggerEnter(index: Int, offsetDp: Float = 24f): Modifier {
    val density = LocalDensity.current
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * AnimSpec.StaggerInterval.toLong())
        progress.animateTo(1f, animationSpec = AnimSpec.EnterTween)
    }
    val px = with(density) { offsetDp.dp.toPx() }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * px
    }
}

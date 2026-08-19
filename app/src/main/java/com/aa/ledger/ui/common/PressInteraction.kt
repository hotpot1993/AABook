package com.aa.ledger.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.aa.ledger.ui.theme.AnimSpec

/**
 * iOS 26「Liquid Glass」按压反馈。
 *
 * 用「按下缩放 + 弹簧回弹」替代 Material 波纹（indication = null），
 * 覆盖所有可点击控件，保证全应用按压手感一致。
 */

/**
 * 按压缩放核心：按下时缩放到 [pressedScale]，松开弹簧回弹到 1.0。
 * 需要调用方传入共享的 [interactionSource]，以便与 clickable / combinedClickable 共用。
 */
@Composable
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    enabled: Boolean = true,
    pressedScale: Float = 0.96f
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale = remember { Animatable(1f) }
    val target = if (pressed && enabled) pressedScale else 1f
    LaunchedEffect(target) {
        val spec = if (target < 1f) AnimSpec.PressSpring else AnimSpec.ReleaseSpring
        scale.animateTo(target, animationSpec = spec)
    }
    return this.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

/** 点击 + 按压缩放（去波纹）。 */
@Composable
fun Modifier.bounceClick(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this
        .pressScale(interactionSource, enabled)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

/** 点击 + 长按（含按压缩放），用于流水行等需要长按删除的场景。 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.bounceCombinedClickable(
    enabled: Boolean = true,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this
        .pressScale(interactionSource, enabled)
        .combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick,
            onLongClick = onLongClick,
            onDoubleClick = onDoubleClick
        )
}

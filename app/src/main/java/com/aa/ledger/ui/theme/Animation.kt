package com.aa.ledger.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * 统一动效参数表 —— iOS 26「Liquid Glass」风格。
 *
 * 所有页面转场、按压反馈、内容入场动画都从这里取值，保证全应用手感一致。
 * 参数按 iOS 参考值落地，真机预览后可在此处统一微调。
 */
object AnimSpec {

    // ═══ 弹簧（物理化，替代线性/默认缓动）═══

    /** 按压反馈：按下时使用，干脆且带轻微回弹。 */
    val PressSpring: SpringSpec<Float> =
        spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)

    /** 松开回弹：比 PressSpring 更明显的 overshoot，模拟「液态」回弹。 */
    val ReleaseSpring: SpringSpec<Float> =
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)

    /** 页面转场：无回弹、低刚度，平滑推入/返回。 */
    val TransitionSpring: SpringSpec<Float> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)

    /** 快速归位：滑动手势、settle 用，无回弹、高刚度。 */
    val SnapSpring: SpringSpec<Float> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)

    // ═══ 缓动曲线（CubicBezier 近似 iOS 标准曲线）═══

    /** iOS easeIn = cubic-bezier(0.42, 0, 1, 1) */
    val IosEaseIn: Easing = CubicBezierEasing(0.42f, 0f, 1f, 1f)

    /** iOS easeOut = cubic-bezier(0, 0, 0.58, 1) */
    val IosEaseOut: Easing = CubicBezierEasing(0f, 0f, 0.58f, 1f)

    /** iOS easeInOut = cubic-bezier(0.42, 0, 0.58, 1) */
    val IosEaseInOut: Easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

    /** iOS 26 强调曲线：更积极、略带冲击力的 easeOut。 */
    val IosEmphasized: Easing = CubicBezierEasing(0.2f, 0f, 0.4f, 1f)

    // ═══ 时长（毫秒）═══

    /** 按压状态变化（按下缩放）时长。 */
    const val PressDuration = 120

    /** 页面转场 / 大块内容移动时长。 */
    const val TransitionDuration = 400

    /** 入场 / 淡入淡出时长。 */
    const val FadeDuration = 300

    /** 多米诺 stagger 入场时，相邻卡片间隔。 */
    const val StaggerInterval = 40

    // ═══ 常用 Tween 动画规格 ═══

    /** 页面推入 / 返回位移。 */
    val PageTween: FiniteAnimationSpec<Float> =
        tween(durationMillis = TransitionDuration, easing = IosEaseOut)

    /** 淡入淡出。 */
    val FadeTween: FiniteAnimationSpec<Float> =
        tween(durationMillis = FadeDuration, easing = IosEaseInOut)

    /** 内容入场（fade + slide）。 */
    val EnterTween: FiniteAnimationSpec<Float> =
        tween(durationMillis = TransitionDuration, easing = IosEaseOut)

    /** 图表 / 数字生长。 */
    val GrowTween: FiniteAnimationSpec<Float> =
        tween(durationMillis = 700, easing = IosEaseOut)
}

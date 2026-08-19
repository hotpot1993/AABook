package com.aa.ledger.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.*; import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*; import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons; import androidx.compose.material.icons.outlined.Delete; import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*; import androidx.compose.runtime.*
import androidx.compose.ui.Alignment; import androidx.compose.ui.Modifier; import androidx.compose.ui.draw.clip; import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput; import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity; import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset; import androidx.compose.ui.unit.dp; import androidx.compose.ui.unit.sp
import com.aa.ledger.ui.theme.*; import kotlinx.coroutines.launch; import kotlin.math.roundToInt

@Composable
fun SwipeActionRow(
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 0,
    content: @Composable () -> Unit
) {
    val singleWidthDp = 80.dp
    val density = LocalDensity.current
    val singleWidthPx = with(density) { singleWidthDp.toPx() }
    val editCount = if (onEdit != null) 1 else 0
    val deleteCount = if (onDelete != null) 1 else 0
    val totalActions = editCount + deleteCount
    if (totalActions == 0) { content(); return }
    val actionWidthPx = singleWidthPx * totalActions
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var rowHeight by remember { mutableIntStateOf(0) }
    val cardShape = if (cornerRadius > 0) RoundedCornerShape(cornerRadius.dp) else RoundedCornerShape(0.dp)

    Box(modifier.then(if (cornerRadius > 0) Modifier.clip(RoundedCornerShape(cornerRadius.dp)) else Modifier)) {
        // 底层操作按钮
        Row(
            Modifier.fillMaxWidth().height(with(density) { rowHeight.toDp() }).padding(vertical = 1.dp),
            horizontalArrangement = Arrangement.End
        ) {
            val cr = cornerRadius
            if (onEdit != null) ActionButton("编辑", SystemBlue, Icons.Outlined.Edit, singleWidthDp, cornerRadius = cr, onClick = {
                scope.launch { offset.animateTo(0f, AnimSpec.SnapSpring) }; onEdit()
            })
            if (onDelete != null) ActionButton("删除", SystemRed, Icons.Outlined.Delete, singleWidthDp, cornerRadius = cr, onClick = {
                scope.launch { offset.animateTo(0f, AnimSpec.SnapSpring) }; onDelete()
            })
        }

        // 上层卡片
        Surface(
            Modifier
                .fillMaxWidth()
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .onSizeChanged { size -> rowHeight = size.height }
                .pointerInput(totalActions) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (offset.value < -actionWidthPx * 0.35f) offset.animateTo(-actionWidthPx, AnimSpec.SnapSpring)
                                else offset.animateTo(0f, AnimSpec.SnapSpring)
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            scope.launch {
                                offset.snapTo((offset.value + dragAmount).coerceIn(-actionWidthPx, 0f))
                            }
                        }
                    )
                }
                .clickable(onClick = onClick),
            shape = cardShape,
            color = MontraSurface,
            shadowElevation = 2.dp
        ) {
            content()
        }
    }
}

@Composable
private fun ActionButton(label: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, width: androidx.compose.ui.unit.Dp, onClick: () -> Unit, cornerRadius: Int = 0) {
    val shape = if (cornerRadius > 0) RoundedCornerShape(cornerRadius.dp) else RoundedCornerShape(0.dp)
    Box(
        Modifier.width(width).fillMaxHeight().clip(shape).background(color).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, label, tint = Color.White, modifier = Modifier.size(20.dp))
            Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

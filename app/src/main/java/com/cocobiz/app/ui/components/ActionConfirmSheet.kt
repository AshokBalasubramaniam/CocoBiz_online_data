package com.cocobiz.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cocobiz.app.R

enum class ConfirmSheetStyle(
    val accent: Color,
    val accentDark: Color,
    val tint: Color,
    val badgeIcon: ImageVector
) {
    COMPLETE(Color(0xFF169B50), Color(0xFF0E6B3A), Color(0xFFE7F8ED), Icons.Default.Check),
    DELETE(Color(0xFFE52525), Color(0xFFB91C1C), Color(0xFFFFF0F0), Icons.Default.Delete)
}

private val SheetTextPrimary = Color(0xFF101B35)
private val SheetTextSecondary = Color(0xFF606B80)

/** Bottom-sheet confirmation with illustration, used for sale complete/delete. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionConfirmSheet(
    style: ConfirmSheetStyle,
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String = "Cancel"
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .size(width = 40.dp, height = 5.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD9DEE3))
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Soft leaves at the sides
            Image(
                painter = painterResource(R.drawable.bg_nav_leaves),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.18f)
            )

            // Close button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F4F6))
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = SheetTextPrimary, modifier = Modifier.size(20.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ConfirmIllustration(style)
                Spacer(Modifier.height(14.dp))
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SheetTextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = message,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = SheetTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.9f),
                        border = BorderStroke(1.dp, style.accent.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(dismissText, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = style.accentDark)
                        }
                    }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .shadow(6.dp, RoundedCornerShape(14.dp), ambientColor = style.accent, spotColor = style.accent)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.horizontalGradient(listOf(style.accentDark, style.accent)))
                            .clickable(onClick = onConfirm),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(style.badgeIcon, contentDescription = null, tint = style.accent, modifier = Modifier.size(15.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(confirmText, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmIllustration(style: ConfirmSheetStyle) {
    Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
        // Halo
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(style.tint, style.tint.copy(alpha = 0.6f))))
        )
        // Sparkle strokes (top-right)
        listOf(Triple(30f, 34.dp, -38.dp), Triple(60f, 42.dp, -26.dp), Triple(0f, 26.dp, -46.dp)).forEach { (angle, x, y) ->
            Box(
                modifier = Modifier
                    .offset(x = x, y = y)
                    .rotate(angle)
                    .size(width = 3.dp, height = 10.dp)
                    .clip(CircleShape)
                    .background(style.accent)
            )
        }
        // Leaves (bottom-left)
        Icon(
            Icons.Default.Eco,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier
                .offset(x = (-40).dp, y = 30.dp)
                .size(30.dp)
        )
        Icon(
            Icons.Default.Eco,
            contentDescription = null,
            tint = Color(0xFF81C784),
            modifier = Modifier
                .offset(x = (-50).dp, y = 14.dp)
                .rotate(-40f)
                .size(20.dp)
        )
        // Document
        Column(
            modifier = Modifier
                .offset(y = (-2).dp)
                .size(width = 50.dp, height = 62.dp)
                .shadow(4.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFE4EBEB), RoundedCornerShape(8.dp))
                .padding(horizontal = 9.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(1f, 0.8f, 0.6f, 0.75f).forEach { fraction ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD8E0E4))
                )
            }
        }
        // Badge (bottom-right of document)
        Box(
            modifier = Modifier
                .offset(x = 24.dp, y = 26.dp)
                .size(34.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(style.accentDark)
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(style.badgeIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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

private val LogoutGreen = Color(0xFF169B50)
private val LogoutGreenDark = Color(0xFF0E6B3A)
private val LogoutMint = Color(0xFFE7F8ED)
private val LogoutTextPrimary = Color(0xFF101B35)
private val LogoutTextSecondary = Color(0xFF606B80)

/** Bottom-sheet logout confirmation, styled like [ActionConfirmSheet]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogoutConfirmSheet(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isLoggingOut: Boolean = false
) {
    val dismiss = { if (!isLoggingOut) onDismiss() }
    ModalBottomSheet(
        onDismissRequest = dismiss,
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(LogoutMint, Color.White)))
        ) {
            Image(
                painter = painterResource(R.drawable.bg_nav_leaves),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.18f)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F4F6))
                    .clickable(onClick = dismiss),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = LogoutTextPrimary, modifier = Modifier.size(20.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LogoutIllustration()
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Logout",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = LogoutTextPrimary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Are you sure you want to logout\nfrom your account?",
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    color = LogoutTextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(18.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF4FAF6))
                        .border(1.dp, Color(0xFFE3EFE7), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    LogoutInfoRow(Icons.Default.Person, LogoutGreen, Color(0xFFDDF3E4), "You will be signed out from this device")
                    HorizontalDivider(color = Color(0xFFE3EFE7))
                    LogoutInfoRow(Icons.Default.Cloud, Color(0xFF3B82F6), Color(0xFFE1ECFE), "Your data will remain safe in your account")
                    HorizontalDivider(color = Color(0xFFE3EFE7))
                    LogoutInfoRow(Icons.AutoMirrored.Filled.Logout, Color(0xFFE53935), Color(0xFFFDE4E4), "You can login again anytime")
                }

                Spacer(Modifier.height(22.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        onClick = dismiss,
                        enabled = !isLoggingOut,
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, LogoutGreen.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Cancel", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LogoutGreenDark)
                        }
                    }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .shadow(6.dp, RoundedCornerShape(14.dp), ambientColor = LogoutGreen, spotColor = LogoutGreen)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.horizontalGradient(listOf(LogoutGreenDark, LogoutGreen)))
                            .clickable(enabled = !isLoggingOut, onClick = onConfirm),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isLoggingOut) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Logout", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogoutInfoRow(icon: ImageVector, iconTint: Color, iconBg: Color, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(text, fontSize = 13.sp, color = LogoutTextPrimary.copy(alpha = 0.8f))
    }
}

@Composable
private fun LogoutIllustration() {
    Box(modifier = Modifier.size(130.dp), contentAlignment = Alignment.Center) {
        // Halo
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFFD4F1DE), LogoutMint.copy(alpha = 0.4f))))
        )
        // Sparkle strokes (top-right)
        listOf(Triple(30f, 40.dp, -44.dp), Triple(60f, 50.dp, -30.dp), Triple(0f, 30.dp, -52.dp)).forEach { (angle, x, y) ->
            Box(
                modifier = Modifier
                    .offset(x = x, y = y)
                    .rotate(angle)
                    .size(width = 3.dp, height = 10.dp)
                    .clip(CircleShape)
                    .background(LogoutGreen)
            )
        }
        // Leaves at both sides
        Icon(
            Icons.Default.Eco,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier
                .offset(x = (-42).dp, y = 30.dp)
                .size(32.dp)
        )
        Icon(
            Icons.Default.Eco,
            contentDescription = null,
            tint = Color(0xFF81C784),
            modifier = Modifier
                .offset(x = 42.dp, y = 32.dp)
                .rotate(80f)
                .size(28.dp)
        )
        // Exit door tile
        Box(
            modifier = Modifier
                .size(width = 58.dp, height = 70.dp)
                .shadow(6.dp, RoundedCornerShape(14.dp), ambientColor = LogoutGreen, spotColor = LogoutGreen)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF2BB463), LogoutGreenDark))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

package com.cocobiz.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Toll
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.cocobiz.app.R
import com.cocobiz.app.domain.model.CoconutType
import com.cocobiz.app.domain.model.SaleStatus
import com.cocobiz.app.domain.model.SalesEntry
import com.cocobiz.app.domain.model.StatusColor
import com.cocobiz.app.presentation.sales.SaleDetailsViewModel
import com.cocobiz.app.ui.theme.AvatarColors
import com.cocobiz.app.ui.theme.StatusGray
import com.cocobiz.app.ui.theme.StatusRed
import com.cocobiz.app.ui.theme.StatusYellow
import com.cocobiz.app.util.toCurrencyString
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val cardDateFmt = DateTimeFormatter.ofPattern("d MMM yyyy")

// Card palette (fixed light design to match the illustration)
private object CardColors {
    val Card = Color(0xFFFFFFFF)
    val TextPrimary = Color(0xFF101B35)
    val TextSecondary = Color(0xFF606B80)
    val Green = Color(0xFF169B50)
    val GreenLight = Color(0xFFE7F8ED)
    val Mint = Color(0xFFF4FBF8)
    val Blue = Color(0xFF1764E8)
    val BlueLight = Color(0xFFE8F0FF)
    val Purple = Color(0xFF9254E9)
    val PurpleLight = Color(0xFFF1E9FD)
    val Orange = Color(0xFFFF5A24)
    val OrangeIcon = Color(0xFFF59E0B)
    val OrangeLight = Color(0xFFFFF2DC)
    val Red = Color(0xFFE52525)
    val RedLight = Color(0xFFFFF0F0)
    val Divider = Color(0xFFE4EBEB)
}

@Composable
fun SalesCard(
    sale: SalesEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    editEnabled: Boolean = sale.status == SaleStatus.ACTIVE
) {
    var showDetails by remember { mutableStateOf(false) }
    if (showDetails) {
        SaleDetailsDialog(sale = sale, onDismiss = { showDetails = false })
    }

    val isActive = sale.status == SaleStatus.ACTIVE
    val statusColor = when (sale.statusColor) {
        StatusColor.GREEN -> CardColors.Green
        StatusColor.YELLOW -> StatusYellow
        StatusColor.RED -> StatusRed
        StatusColor.GRAY -> StatusGray
    }
    val ringColor by animateColorAsState(
        targetValue = statusColor,
        animationSpec = tween(300),
        label = "statusColor"
    )

    val avatarColor = AvatarColors[(sale.id % AvatarColors.size).toInt()]
    val initials = sale.dealerName.trim().split(" ")
        .filter { it.isNotEmpty() }
        .take(2).joinToString("") { it.first().uppercaseChar().toString() }
        .ifEmpty { "?" }

    val unitLabel = if (sale.coconutType == CoconutType.TONNAGE) "Tons" else "Pcs"
    val rateLabel = if (sale.coconutType == CoconutType.TONNAGE) "/Ton" else "/Pc"

    val progress = if (sale.cycleDays > 0) {
        (sale.remainingDays.toFloat() / sale.cycleDays.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val saleDaysAgo = ChronoUnit.DAYS.between(sale.salesDate, LocalDate.now())
    val saleDateHint = when {
        saleDaysAgo == 0L -> "(Today)"
        saleDaysAgo == 1L -> "(Yesterday)"
        saleDaysAgo > 1L -> "($saleDaysAgo days ago)"
        else -> "(In ${-saleDaysAgo} days)"
    }
    val nextDateHint = when {
        !isActive -> ""
        sale.isOverdue -> "(Overdue)"
        sale.remainingDays == 0L -> "(Today)"
        sale.remainingDays == 1L -> "(Tomorrow)"
        else -> "(In ${sale.remainingDays} days)"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(24.dp), ambientColor = CardColors.Green.copy(alpha = 0.25f), spotColor = CardColors.Green.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = CardColors.Card)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            // Wide screens: ring sits beside the metrics panel like the reference;
            // phones: ring moves into the header so all four metrics fit.
            val wide = maxWidth >= 480.dp

            // ── Illustration (top-right) ──────────────────────────────
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .fillMaxWidth(if (wide) 0.55f else 0.64f)
                    .height(if (wide) 170.dp else 124.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.bg_sale_card),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.CenterEnd,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                0f to CardColors.Card,
                                0.3f to CardColors.Card.copy(alpha = 0.55f),
                                0.65f to Color.Transparent
                            )
                        )
                )
            }

            // ── Three-dot options menu ───────────────────────────────
            CardOptionsMenu(
                isActive = isActive,
                editEnabled = editEnabled,
                onEdit = onEdit,
                onDelete = onDelete,
                onComplete = onComplete,
                onView = { showDetails = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 10.dp)
            )

            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {

                // ── Header ───────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (wide) 96.dp else 84.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CardHeader(
                        initials = initials,
                        avatarColor = avatarColor,
                        name = sale.dealerName,
                        place = sale.dealerPlace,
                        isActive = isActive,
                        avatarSize = if (wide) 72.dp else 54.dp,
                        onAvatarClick = { showDetails = true },
                        modifier = Modifier.weight(1f)
                    )
                    if (!wide) {
                        DaysLeftRing(
                            progress = progress,
                            remainingDays = sale.remainingDays,
                            color = if (isActive) ringColor else StatusGray,
                            size = 66.dp,
                            modifier = Modifier
                                .align(Alignment.Bottom)
                                .offset(y = 10.dp)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // ── Metrics + ring ───────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MetricsPanel(
                        typeLabel = if (sale.coconutType == CoconutType.SINGLE_PIECE) "Single" else sale.coconutType.displayName,
                        quantity = "${sale.quantity.toFormattedString()} $unitLabel",
                        rate = "${sale.rate.toRupeeString()}$rateLabel",
                        total = sale.totalAmount.toCurrencyString(),
                        modifier = Modifier.weight(1f)
                    )
                    if (wide) {
                        Spacer(Modifier.width(16.dp))
                        DaysLeftRing(
                            progress = progress,
                            remainingDays = sale.remainingDays,
                            color = if (isActive) ringColor else StatusGray,
                            size = 110.dp
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // ── Dates + market rate ──────────────────────────────
                Panel(background = CardColors.Mint, elevated = false) {
                    DateCell(
                        icon = Icons.Outlined.CalendarMonth,
                        tint = CardColors.Green,
                        tintBg = CardColors.GreenLight,
                        label = if (isActive) "Sale Date" else "Completed On",
                        value = sale.salesDate.format(cardDateFmt),
                        hint = saleDateHint,
                        modifier = Modifier.weight(1f)
                    )
                    PanelDivider()
                    DateCell(
                        icon = Icons.Outlined.CalendarMonth,
                        tint = CardColors.Blue,
                        tintBg = CardColors.BlueLight,
                        label = "Next Date",
                        value = sale.nextSalesDate.format(cardDateFmt),
                        hint = nextDateHint,
                        hintColor = if (sale.isOverdue) CardColors.Red else CardColors.TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    PanelDivider()
                    MarketRateCell(
                        marketRate = sale.marketRate,
                        rate = sale.rate,
                        rateLabel = rateLabel,
                        modifier = Modifier.weight(1f)
                    )
                }

            }
        }
    }
}

// ── Header ──────────────────────────────────────────────────────────────

@Composable
private fun CardHeader(
    initials: String,
    avatarColor: Color,
    name: String,
    place: String,
    isActive: Boolean,
    avatarSize: Dp,
    modifier: Modifier = Modifier,
    onAvatarClick: (() -> Unit)? = null
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(avatarSize)
                .shadow(8.dp, CircleShape, ambientColor = avatarColor, spotColor = avatarColor)
                .clip(CircleShape)
                .then(if (onAvatarClick != null) Modifier.clickable(onClick = onAvatarClick) else Modifier)
                .background(Brush.linearGradient(listOf(avatarColor.copy(alpha = 0.82f), avatarColor))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                fontSize = (avatarSize.value * 0.4f).sp
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = name,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CardColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = CardColors.TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = place,
                    fontSize = 13.sp,
                    color = CardColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(5.dp))
            StatusPill(isActive = isActive)
        }
    }
}

@Composable
private fun StatusPill(isActive: Boolean) {
    val color = if (isActive) CardColors.Green else StatusGray
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isActive) CardColors.GreenLight else StatusGray.copy(alpha = 0.14f))
            .padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = if (isActive) "Active" else "Completed",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun CardOptionsMenu(
    isActive: Boolean,
    editEnabled: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onComplete: () -> Unit,
    onView: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(CardColors.Card)
                .clickable { expanded = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = CardColors.TextPrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        if (expanded) {
            val density = LocalDensity.current
            Popup(
                alignment = Alignment.TopEnd,
                offset = with(density) { IntOffset(0, 40.dp.roundToPx()) },
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true)
            ) {
                Column(
                    modifier = Modifier
                        .width(250.dp)
                        .shadow(16.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardColors.Card)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    fun select(action: () -> Unit) { expanded = false; action() }
                    MenuOption(
                        icon = Icons.Outlined.Visibility,
                        title = "View",
                        subtitle = "See full sale details",
                        tint = CardColors.Purple,
                        background = Color(0xFFF6F0FE),
                        iconBackground = Color(0xFFE9DCFC),
                        onClick = { select(onView) }
                    )
                    if (editEnabled) {
                        MenuOption(
                            icon = Icons.Default.Edit,
                            title = "Edit",
                            subtitle = "Update sale details",
                            tint = CardColors.Blue,
                            background = CardColors.BlueLight.copy(alpha = 0.7f),
                            iconBackground = Color(0xFFD6E4FF),
                            onClick = { select(onEdit) }
                        )
                    }
                    if (isActive) {
                        MenuOption(
                            icon = Icons.Default.Check,
                            title = "Mark as Completed",
                            subtitle = "Move to completed",
                            tint = CardColors.Green,
                            background = CardColors.GreenLight.copy(alpha = 0.8f),
                            iconBackground = Color(0xFFCDEFD9),
                            onClick = { select(onComplete) }
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .height(1.dp)
                            .background(CardColors.Divider)
                    )
                    MenuOption(
                        icon = Icons.Default.Delete,
                        title = "Delete",
                        subtitle = "Remove this sale",
                        tint = CardColors.Red,
                        titleColor = CardColors.Red,
                        background = CardColors.RedLight,
                        iconBackground = Color(0xFFFFDCDC),
                        onClick = { select(onDelete) }
                    )
                }
            }
        }
    }

}

@Composable
private fun MenuOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    background: Color,
    iconBackground: Color,
    onClick: () -> Unit,
    titleColor: Color = CardColors.TextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon, tint, iconBackground, 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = titleColor, maxLines = 1)
            Text(text = subtitle, fontSize = 12.sp, color = CardColors.TextSecondary, maxLines = 1)
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = if (titleColor == CardColors.Red) CardColors.Red else CardColors.TextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ── Days-left ring ──────────────────────────────────────────────────────

@Composable
private fun DaysLeftRing(
    progress: Float,
    remainingDays: Long,
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800),
        label = "daysLeft"
    )
    val stroke = size * 0.075f
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .shadow(12.dp, CircleShape, ambientColor = color, spotColor = color)
            .clip(CircleShape)
            .background(CardColors.Card)
            .padding(size * 0.06f)
    ) {
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.fillMaxSize(),
            color = color.copy(alpha = 0.14f),
            strokeWidth = stroke,
            strokeCap = StrokeCap.Round
        )
        CircularProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxSize(),
            color = color,
            strokeWidth = stroke,
            strokeCap = StrokeCap.Round
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$remainingDays",
                fontWeight = FontWeight.ExtraBold,
                color = color,
                fontSize = (size.value * 0.3f).sp,
                lineHeight = (size.value * 0.3f).sp
            )
            Text(
                text = "days left",
                color = CardColors.TextSecondary,
                fontSize = (size.value * 0.13f).coerceAtLeast(9f).sp,
                lineHeight = (size.value * 0.15f).coerceAtLeast(11f).sp
            )
        }
    }
}

// ── Panels & cells ──────────────────────────────────────────────────────

@Composable
private fun Panel(
    modifier: Modifier = Modifier,
    background: Color = CardColors.Card,
    elevated: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (elevated) Modifier.shadow(4.dp, shape, ambientColor = CardColors.Green.copy(alpha = 0.2f), spotColor = CardColors.Green.copy(alpha = 0.2f)) else Modifier)
            .clip(shape)
            .background(background)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun PanelDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(40.dp)
            .background(CardColors.Divider)
    )
}

@Composable
private fun IconBadge(icon: ImageVector, tint: Color, background: Color, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.55f))
    }
}

@Composable
private fun MetricsPanel(
    typeLabel: String,
    quantity: String,
    rate: String,
    total: String,
    modifier: Modifier = Modifier
) {
    Panel(modifier = modifier, background = CardColors.Card.copy(alpha = 0.96f)) {
        MetricCell(Icons.Default.Eco, CardColors.Green, CardColors.GreenLight, "Type", typeLabel, Modifier.weight(1f))
        PanelDivider()
        MetricCell(Icons.Outlined.ViewInAr, CardColors.Blue, CardColors.BlueLight, "Quantity", quantity, Modifier.weight(1f))
        PanelDivider()
        MetricCell(Icons.Default.Toll, CardColors.Purple, CardColors.PurpleLight, "Rate", rate, Modifier.weight(1.1f))
        PanelDivider()
        MetricCell(
            Icons.Default.CurrencyRupee, CardColors.OrangeIcon, CardColors.OrangeLight,
            "Total Amount", total, Modifier.weight(1.2f), valueColor = CardColors.Orange
        )
    }
}

@Composable
private fun MetricCell(
    icon: ImageVector,
    tint: Color,
    tintBg: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = CardColors.TextPrimary
) {
    Column(
        modifier = modifier.padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconBadge(icon, tint, tintBg, 30.dp)
        Spacer(Modifier.height(4.dp))
        Text(text = label, fontSize = 10.sp, color = CardColors.TextSecondary, maxLines = 1)
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun InfoCell(
    icon: ImageVector,
    tint: Color,
    tintBg: Color,
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.padding(horizontal = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, tint, tintBg, 26.dp)
            Spacer(Modifier.width(6.dp))
            Text(text = label, fontSize = 10.sp, color = CardColors.TextSecondary, maxLines = 1)
        }
        Spacer(Modifier.height(3.dp))
        content()
    }
}

@Composable
private fun DateCell(
    icon: ImageVector,
    tint: Color,
    tintBg: Color,
    label: String,
    value: String,
    hint: String,
    modifier: Modifier = Modifier,
    hintColor: Color = CardColors.TextSecondary
) {
    InfoCell(icon, tint, tintBg, label, modifier) {
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = CardColors.TextPrimary,
            maxLines = 1
        )
        if (hint.isNotEmpty()) {
            Text(text = hint, fontSize = 10.sp, color = hintColor, maxLines = 1)
        }
    }
}

@Composable
private fun MarketRateCell(
    marketRate: Double,
    rate: Double,
    rateLabel: String,
    modifier: Modifier = Modifier
) {
    InfoCell(Icons.Default.BarChart, CardColors.Green, CardColors.GreenLight, "Market Rate", modifier) {
        Text(
            text = if (marketRate > 0) "${marketRate.toRupeeString()}$rateLabel" else "—",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = CardColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (marketRate > 0 && rate > 0) {
            val change = (marketRate - rate) / rate * 100
            val up = change >= 0
            val color = if (up) CardColors.Green else CardColors.Red
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (up) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = "${if (up) "+" else ""}${"%.1f".format(change)}%",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
            }
        }
    }
}

// ── Full-screen sale details ────────────────────────────────────────────

@Composable
private fun SaleDetailsDialog(sale: SalesEntry, onDismiss: () -> Unit) {
    val isActive = sale.status == SaleStatus.ACTIVE
    val unitLabel = if (sale.coconutType == CoconutType.TONNAGE) "Tons" else "Pcs"
    val rateLabel = if (sale.coconutType == CoconutType.TONNAGE) "/Ton" else "/Pc"
    val avatarColor = AvatarColors[(sale.id % AvatarColors.size).toInt()]
    val initials = sale.dealerName.trim().split(" ")
        .filter { it.isNotEmpty() }
        .take(2).joinToString("") { it.first().uppercaseChar().toString() }
        .ifEmpty { "?" }
    val ringColor = when (sale.statusColor) {
        StatusColor.GREEN -> CardColors.Green
        StatusColor.YELLOW -> StatusYellow
        StatusColor.RED -> StatusRed
        StatusColor.GRAY -> StatusGray
    }
    val progress = if (sale.cycleDays > 0) {
        (sale.remainingDays.toFloat() / sale.cycleDays.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val stampFmt = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a")
    fun Long.asStamp(): String =
        java.time.Instant.ofEpochMilli(this).atZone(java.time.ZoneId.systemDefault()).format(stampFmt)

    val detailsViewModel: SaleDetailsViewModel = hiltViewModel(key = "saleDetails_${sale.id}")
    var previous by remember { mutableStateOf<SalesEntry?>(null) }
    var previousLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(sale) {
        previous = if (sale.marketRate > 0) detailsViewModel.previousMarketRateSale(sale) else null
        previousLoaded = true
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFEEF9F7))
                .systemBarsPadding()
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = CardColors.TextPrimary
                    )
                }
                Text(
                    text = "Sale Details",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = CardColors.TextPrimary
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Hero
                DetailSection {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(R.drawable.bg_sale_card),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.CenterEnd,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .fillMaxWidth(0.6f)
                                .height(120.dp)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .fillMaxWidth(0.6f)
                                .height(120.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        0f to CardColors.Card,
                                        0.4f to CardColors.Card.copy(alpha = 0.4f),
                                        0.7f to Color.Transparent
                                    )
                                )
                        )
                        Column(modifier = Modifier.padding(16.dp)) {
                            CardHeader(
                                initials = initials,
                                avatarColor = avatarColor,
                                name = sale.dealerName,
                                place = sale.dealerPlace,
                                isActive = isActive,
                                avatarSize = 68.dp
                            )
                            Spacer(Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                DaysLeftRing(
                                    progress = progress,
                                    remainingDays = sale.remainingDays,
                                    color = if (isActive) ringColor else StatusGray,
                                    size = 96.dp
                                )
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text("Total Amount", fontSize = 13.sp, color = CardColors.TextSecondary)
                                    Text(
                                        sale.totalAmount.toCurrencyString(),
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = CardColors.Orange
                                    )
                                    Text(
                                        "${sale.quantity.toFormattedString()} $unitLabel × ${sale.rate.toRupeeString()}$rateLabel",
                                        fontSize = 13.sp,
                                        color = CardColors.TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Sale summary
                DetailSection(title = "Sale Summary") {
                    DetailRow(Icons.Default.Eco, CardColors.Green, CardColors.GreenLight, "Coconut Type", sale.coconutType.displayName)
                    DetailRow(Icons.Outlined.ViewInAr, CardColors.Blue, CardColors.BlueLight, "Quantity", "${sale.quantity.toFormattedString()} $unitLabel")
                    DetailRow(Icons.Default.Toll, CardColors.Purple, CardColors.PurpleLight, "Rate", "${sale.rate.toRupeeString()}$rateLabel")
                    DetailRow(
                        Icons.Default.CurrencyRupee, CardColors.OrangeIcon, CardColors.OrangeLight,
                        "Total Amount", sale.totalAmount.toCurrencyString(), valueColor = CardColors.Orange, showDivider = false
                    )
                }

                // Market: current vs previous market rate
                DetailSection(title = "Market Rate") {
                    if (sale.marketRate > 0) {
                        DetailRow(
                            Icons.Default.BarChart, CardColors.Green, CardColors.GreenLight,
                            "Current Market Rate\n(${sale.salesDate.format(cardDateFmt)})",
                            "${sale.marketRate.toRupeeString()}$rateLabel",
                            showDivider = previous != null || !previousLoaded
                        )
                        val prev = previous
                        when {
                            !previousLoaded -> Unit
                            prev == null -> Text(
                                "No previous market rate to compare.",
                                fontSize = 13.sp,
                                color = CardColors.TextSecondary,
                                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                            )
                            else -> {
                                val rateDiff = sale.marketRate - prev.marketRate
                                val change = rateDiff / prev.marketRate * 100
                                val up = rateDiff >= 0
                                val sign = if (up) "+" else "-"
                                val diffColor = if (up) CardColors.Green else CardColors.Red
                                val diffIcon = if (up) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward
                                DetailRow(
                                    Icons.Outlined.CalendarMonth, CardColors.Purple, CardColors.PurpleLight,
                                    "Previous Market Rate\n(${prev.salesDate.format(cardDateFmt)})",
                                    "${prev.marketRate.toRupeeString()}$rateLabel"
                                )
                                DetailRow(
                                    diffIcon, diffColor, diffColor.copy(alpha = 0.12f),
                                    "Rate Difference",
                                    "$sign${kotlin.math.abs(rateDiff).toRupeeString()}$rateLabel",
                                    valueColor = diffColor
                                )
                                DetailRow(
                                    diffIcon, diffColor, diffColor.copy(alpha = 0.12f),
                                    "Percentage Change",
                                    "$sign${"%.1f".format(kotlin.math.abs(change))}%",
                                    valueColor = diffColor,
                                    showDivider = false
                                )
                            }
                        }
                    } else {
                        Text(
                            "No market rate entered for this sale.",
                            fontSize = 14.sp,
                            color = CardColors.TextSecondary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                // Dates
                DetailSection(title = "Schedule") {
                    DetailRow(Icons.Outlined.CalendarMonth, CardColors.Green, CardColors.GreenLight, if (isActive) "Sale Date" else "Completed On", sale.salesDate.format(cardDateFmt))
                    DetailRow(Icons.Outlined.CalendarMonth, CardColors.Blue, CardColors.BlueLight, "Next Date", sale.nextSalesDate.format(cardDateFmt))
                    DetailRow(Icons.Default.Eco, CardColors.Purple, CardColors.PurpleLight, "Cycle", "${sale.cycleDays} days")
                    DetailRow(
                        Icons.Outlined.CalendarMonth, ringColor, ringColor.copy(alpha = 0.12f),
                        "Days Left",
                        when {
                            !isActive -> "Completed"
                            sale.isOverdue -> "Overdue"
                            else -> "${sale.remainingDays} days"
                        },
                        valueColor = if (isActive) ringColor else StatusGray,
                        showDivider = false
                    )
                }

                if (sale.notes.isNotBlank()) {
                    DetailSection(title = "Notes") {
                        Text(sale.notes, fontSize = 14.sp, color = CardColors.TextPrimary, lineHeight = 20.sp)
                    }
                }

                DetailSection(title = "Record") {
                    DetailRow(Icons.Outlined.CalendarMonth, CardColors.TextSecondary, CardColors.Divider, "Created", sale.createdAt.asStamp())
                    DetailRow(Icons.Default.Edit, CardColors.TextSecondary, CardColors.Divider, "Last Updated", sale.updatedAt.asStamp(), showDivider = false)
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun DetailSection(title: String? = null, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp), ambientColor = CardColors.Green.copy(alpha = 0.2f), spotColor = CardColors.Green.copy(alpha = 0.2f))
            .clip(RoundedCornerShape(20.dp))
            .background(CardColors.Card)
            .then(if (title != null) Modifier.padding(16.dp) else Modifier)
    ) {
        if (title != null) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CardColors.TextPrimary)
            Spacer(Modifier.height(8.dp))
        }
        content()
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    tint: Color,
    tintBg: Color,
    label: String,
    value: String,
    valueColor: Color = CardColors.TextPrimary,
    showDivider: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon, tint, tintBg, 36.dp)
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 14.sp, color = CardColors.TextSecondary, modifier = Modifier.weight(1f))
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
    if (showDivider) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 48.dp)
                .height(1.dp)
                .background(CardColors.Divider)
        )
    }
}

private fun Double.toFormattedString(): String =
    if (this == kotlin.math.floor(this)) "%.0f".format(this) else "%.1f".format(this)

private fun Double.toRupeeString(): String =
    "₹" + java.text.NumberFormat.getNumberInstance(java.util.Locale("en", "IN")).apply {
        maximumFractionDigits = 2
    }.format(this)

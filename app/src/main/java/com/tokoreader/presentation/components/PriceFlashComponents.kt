package com.tokoreader.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokoreader.domain.model.PriceTickDirection
import com.tokoreader.presentation.components.CryptoUtils.formatCryptoPrice
import com.tokoreader.ui.theme.ErrorRed
import com.tokoreader.ui.theme.SuccessGreen
import kotlinx.coroutines.delay

/**
 * Live Price Text with sub-second real-time Color Flash:
 * - Flashes NEON GREEN when price goes up (Market Buy Order executed)
 * - Flashes NEON RED when price goes down (Market Sell Order executed)
 * - Smoothly fades back to default neutral color within 500ms
 */
@Composable
fun LivePriceFlashText(
    price: Double,
    symbol: String,
    modifier: Modifier = Modifier,
    tickDirection: PriceTickDirection = PriceTickDirection.NEUTRAL,
    defaultColor: Color = MaterialTheme.colorScheme.onBackground,
    fontSize: androidx.compose.ui.unit.TextUnit = 26.sp,
    fontWeight: FontWeight = FontWeight.ExtraBold
) {
    var prevPrice by remember { mutableDoubleStateOf(price) }
    var flashState by remember { mutableStateOf(PriceTickDirection.NEUTRAL) }

    LaunchedEffect(price, tickDirection) {
        if (price > 0.0 && prevPrice > 0.0) {
            if (price > prevPrice || tickDirection == PriceTickDirection.UP) {
                flashState = PriceTickDirection.UP
            } else if (price < prevPrice || tickDirection == PriceTickDirection.DOWN) {
                flashState = PriceTickDirection.DOWN
            }
        }
        prevPrice = price

        // Reset flash back to neutral after 500ms
        if (flashState != PriceTickDirection.NEUTRAL) {
            delay(500)
            flashState = PriceTickDirection.NEUTRAL
        }
    }

    val targetColor = when (flashState) {
        PriceTickDirection.UP -> SuccessGreen
        PriceTickDirection.DOWN -> ErrorRed
        PriceTickDirection.NEUTRAL -> defaultColor
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 350),
        label = "priceColorAnimation"
    )

    Text(
        text = formatCryptoPrice(symbol, price),
        color = animatedColor,
        fontSize = fontSize,
        fontWeight = fontWeight,
        modifier = modifier
    )
}

/**
 * Live Tick Flash Badge showing instant tick direction (Buy / Sell Pulse)
 */
@Composable
fun LiveTickFlashBadge(
    tickDirection: PriceTickDirection,
    modifier: Modifier = Modifier
) {
    val isUp = tickDirection == PriceTickDirection.UP
    val isDown = tickDirection == PriceTickDirection.DOWN

    val targetBgColor = when {
        isUp -> SuccessGreen.copy(alpha = 0.25f)
        isDown -> ErrorRed.copy(alpha = 0.25f)
        else -> Color(0xFF1E293B)
    }

    val targetBorderColor = when {
        isUp -> SuccessGreen
        isDown -> ErrorRed
        else -> Color.Transparent
    }

    val targetTextColor = when {
        isUp -> SuccessGreen
        isDown -> ErrorRed
        else -> Color(0xFF94A3B8)
    }

    val animatedBg by animateColorAsState(targetValue = targetBgColor, animationSpec = tween(300), label = "badgeBg")
    val animatedBorder by animateColorAsState(targetValue = targetBorderColor, animationSpec = tween(300), label = "badgeBorder")

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(animatedBg)
            .border(1.dp, animatedBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (isUp) {
            Icon(Icons.Filled.ArrowUpward, contentDescription = "Buy Tick", tint = SuccessGreen, modifier = Modifier.size(12.dp))
            Text("BUY TICK", color = targetTextColor, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
        } else if (isDown) {
            Icon(Icons.Filled.ArrowDownward, contentDescription = "Sell Tick", tint = ErrorRed, modifier = Modifier.size(12.dp))
            Text("SELL TICK", color = targetTextColor, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
        } else {
            Text("LIVE", color = targetTextColor, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

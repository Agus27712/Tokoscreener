package com.tokoreader.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.tokoreader.ui.theme.ErrorRed
import com.tokoreader.ui.theme.SuccessGreen
import kotlinx.coroutines.delay

/**
 * Animates price numbers with a smooth continuous counter transition
 * and brief color flash on upward or downward ticks.
 */
@Composable
fun SmoothPriceCounterText(
    price: Double,
    symbol: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    defaultColor: Color = Color.White
) {
    var previousPrice by remember { mutableDoubleStateOf(price) }
    var flashColor by remember { mutableStateOf(defaultColor) }

    LaunchedEffect(price) {
        if (price > 0.0 && previousPrice > 0.0 && price != previousPrice) {
            flashColor = if (price > previousPrice) SuccessGreen else ErrorRed
            previousPrice = price
            delay(400)
            flashColor = defaultColor
        } else if (previousPrice == 0.0 && price > 0.0) {
            previousPrice = price
        }
    }

    val animatedPrice by animateFloatAsState(
        targetValue = price.toFloat(),
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "SmoothPriceCounter"
    )

    val animatedTextColor by animateColorAsState(
        targetValue = flashColor,
        animationSpec = tween(durationMillis = 300),
        label = "SmoothPriceColor"
    )

    val formattedPrice = remember(animatedPrice, symbol) {
        CryptoUtils.formatCryptoPrice(symbol, animatedPrice.toDouble())
    }

    Text(
        text = formattedPrice,
        color = animatedTextColor,
        fontSize = fontSize,
        fontWeight = fontWeight,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

package com.tokoreader.presentation.radar.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokoreader.ui.theme.SuccessGreen

@Composable
fun SequentialStepperWithProgress(
    step1Pass: Boolean,
    step1Detail: String,
    step2Pass: Boolean,
    step2Detail: String,
    step3Pass: Boolean,
    step3Detail: String,
    step4Pass: Boolean,
    step4Detail: String,
    progress1To2: Float,
    progress2To3: Float,
    progress3To4: Float
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Stepper Nodes with Progress Bars between them
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // STEP 1: Bias Trend
            StepNode(
                stepNumber = "1",
                label = "Bias",
                isPass = step1Pass,
                isLocked = false
            )

            // Progress Bar between 1 and 2
            StepConnectorProgressBar(
                progress = progress1To2,
                modifier = Modifier.weight(1f)
            )

            // STEP 2: Setup
            StepNode(
                stepNumber = "2",
                label = "Setup",
                isPass = step2Pass,
                isLocked = !step1Pass
            )

            // Progress Bar between 2 and 3
            StepConnectorProgressBar(
                progress = progress2To3,
                modifier = Modifier.weight(1f)
            )

            // STEP 3: Trigger
            StepNode(
                stepNumber = "3",
                label = "Trigger",
                isPass = step3Pass,
                isLocked = !step1Pass || !step2Pass
            )

            // Progress Bar between 3 and 4
            StepConnectorProgressBar(
                progress = progress3To4,
                modifier = Modifier.weight(1f)
            )

            // STEP 4: Entry
            StepNode(
                stepNumber = "4",
                label = "Entry",
                isPass = step4Pass,
                isLocked = !step1Pass || !step2Pass || !step3Pass
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Sequential step detail descriptions (live from WebSocket indicators)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = step1Detail,
                color = if (step1Pass) SuccessGreen else Color(0xFF94A3B8),
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = step2Detail,
                color = if (step2Pass) SuccessGreen else if (!step1Pass) Color(0xFF475569) else Color(0xFFF59E0B),
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = step3Detail,
                color = if (step3Pass) SuccessGreen else if (!step1Pass || !step2Pass) Color(0xFF475569) else Color(0xFFF59E0B),
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = step4Detail,
                color = if (step4Pass) SuccessGreen else if (!step1Pass || !step2Pass || !step3Pass) Color(0xFF475569) else Color(0xFFF59E0B),
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun StepConnectorProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 350),
        label = "StepProgress"
    )

    Box(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .height(4.dp)
            .background(Color(0xFF1E293B), RoundedCornerShape(2.dp))
    ) {
        if (animatedProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .background(SuccessGreen, RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
fun StepNode(
    stepNumber: String,
    label: String,
    isPass: Boolean,
    isLocked: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val bgColor = when {
            isPass -> SuccessGreen
            isLocked -> Color(0xFF1E293B)
            else -> Color(0xFFF59E0B)
        }
        val contentColor = when {
            isPass -> Color.White
            isLocked -> Color(0xFF64748B)
            else -> Color.Black
        }

        Box(
            modifier = Modifier
                .size(24.dp)
                .background(bgColor, CircleShape)
                .border(
                    width = 1.dp,
                    color = if (isPass) SuccessGreen.copy(alpha = 0.5f) else Color(0xFF334155),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isPass) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Pass",
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            } else if (isLocked) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Locked",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(12.dp)
                )
            } else {
                Text(
                    text = stepNumber,
                    color = contentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = if (isPass) Color.White else if (isLocked) Color(0xFF64748B) else Color(0xFFCBD5E1),
            fontSize = 10.sp,
            fontWeight = if (isPass) FontWeight.Bold else FontWeight.Medium
        )
    }
}

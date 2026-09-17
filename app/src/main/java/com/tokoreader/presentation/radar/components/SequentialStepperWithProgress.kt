package com.tokoreader.presentation.radar.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokoreader.ui.theme.ErrorRed
import com.tokoreader.ui.theme.SuccessGreen

data class StepInfoData(
    val stepIndex: Int, // 1 to 4
    val title: String,
    val detail: String,
    val isPass: Boolean,
    val isLocked: Boolean,
    val helperDescription: String
)

/**
 * Stepper berurutan 4-langkah yang rapi dan menyatu dengan Box keterangan di bawahnya
 * yang dilengkapi animasi 3D Flip saat perpindahan/perubahan step aktif.
 */
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
    // Menentukan step aktif otomatis berdasarkan validasi pipeline
    val autoActiveStep = when {
        !step1Pass -> 1
        !step2Pass -> 2
        !step3Pass -> 3
        !step4Pass -> 4
        else -> 4
    }

    // State untuk step yang dipilih (default mengikuti autoActiveStep)
    var selectedStep by remember { mutableIntStateOf(autoActiveStep) }

    // Selalu update jika autoActiveStep bergeser
    LaunchedEffect(autoActiveStep) {
        selectedStep = autoActiveStep
    }

    // Data 4 langkah stepper
    val step1Data = StepInfoData(
        stepIndex = 1,
        title = "Step 1: Bias Tren (Higher TF)",
        detail = step1Detail,
        isPass = step1Pass,
        isLocked = false,
        helperDescription = "Validasi arah tren utama menggunakan EMA 20/50/200 dan MACD Histogram."
    )
    val step2Data = StepInfoData(
        stepIndex = 2,
        title = "Step 2: Area Setup & Momentum",
        detail = step2Detail,
        isPass = step2Pass,
        isLocked = !step1Pass,
        helperDescription = "Konfirmasi zona akumulasi RSI & Stochastic Oscillator tidak Overbought."
    )
    val step3Data = StepInfoData(
        stepIndex = 3,
        title = "Step 3: Trigger & Tekanan Volume",
        detail = step3Detail,
        isPass = step3Pass,
        isLocked = !step1Pass || !step2Pass,
        helperDescription = "Breakout konfirmasi volume + dominasi Bid pada Order Book Tokocrypto."
    )
    val step4Data = StepInfoData(
        stepIndex = 4,
        title = "Step 4: Validasi Entry & Risk-Reward",
        detail = step4Detail,
        isPass = step4Pass,
        isLocked = !step1Pass || !step2Pass || !step3Pass,
        helperDescription = "Kalkulasi Risk-to-Reward minimum 1:2 dan estimasi Take Profit / Cut Loss."
    )

    val currentTargetData = when (selectedStep) {
        1 -> step1Data
        2 -> step2Data
        3 -> step3Data
        4 -> step4Data
        else -> step1Data
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        // Track: Garis dan Lingkaran Stepper Menyatu
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepNodeSlot(
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedStep = 1 },
                stepNumber = "1",
                isPass = step1Pass,
                isLocked = false,
                isSelected = selectedStep == 1,
                hasLeftLine = false,
                hasRightLine = true,
                leftLinePassed = false,
                rightLineProgress = progress1To2,
                rightLinePassed = step1Pass
            )
            StepNodeSlot(
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedStep = 2 },
                stepNumber = "2",
                isPass = step2Pass,
                isLocked = !step1Pass,
                isSelected = selectedStep == 2,
                hasLeftLine = true,
                hasRightLine = true,
                leftLinePassed = step1Pass,
                rightLineProgress = progress2To3,
                rightLinePassed = step2Pass
            )
            StepNodeSlot(
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedStep = 3 },
                stepNumber = "3",
                isPass = step3Pass,
                isLocked = !step1Pass || !step2Pass,
                isSelected = selectedStep == 3,
                hasLeftLine = true,
                hasRightLine = true,
                leftLinePassed = step2Pass,
                rightLineProgress = progress3To4,
                rightLinePassed = step3Pass
            )
            StepNodeSlot(
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedStep = 4 },
                stepNumber = "4",
                isPass = step4Pass,
                isLocked = !step1Pass || !step2Pass || !step3Pass,
                isSelected = selectedStep == 4,
                hasLeftLine = true,
                hasRightLine = false,
                leftLinePassed = step3Pass,
                rightLineProgress = 0f,
                rightLinePassed = false
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Label ringkas di bawah node (hanya judul)
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            StepTextColumn(
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedStep = 1 },
                title = "Bias",
                isPass = step1Pass,
                isLocked = false,
                isSelected = selectedStep == 1
            )
            StepTextColumn(
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedStep = 2 },
                title = "Setup",
                isPass = step2Pass,
                isLocked = !step1Pass,
                isSelected = selectedStep == 2
            )
            StepTextColumn(
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedStep = 3 },
                title = "Trigger",
                isPass = step3Pass,
                isLocked = !step1Pass || !step2Pass,
                isSelected = selectedStep == 3
            )
            StepTextColumn(
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedStep = 4 },
                title = "Entry",
                isPass = step4Pass,
                isLocked = !step1Pass || !step2Pass || !step3Pass,
                isSelected = selectedStep == 4
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Box Keterangan Stepper dengan Animasi Flip 3D Up-Down (Vertical) saat perpindahan
        FlipStepDetailBox(stepData = currentTargetData)
    }
}

/**
 * Box keterangan dengan animasi Flip 3D Up-Down (RotationX) saat nilai step atau status berubah.
 */
@Composable
private fun FlipStepDetailBox(
    stepData: StepInfoData
) {
    val rotationX = remember { Animatable(0f) }
    var displayedData by remember { mutableStateOf(stepData) }

    // Trigger animasi flip vertikal (up-down) ketika stepData berubah (nomor step atau nilai status)
    LaunchedEffect(stepData.stepIndex, stepData.detail, stepData.isPass, stepData.isLocked) {
        if (stepData != displayedData) {
            // Fase 1: Putar dari 0 ke 90 derajat ke atas/bawah (menghilang)
            rotationX.animateTo(
                targetValue = 90f,
                animationSpec = tween(durationMillis = 140, easing = FastOutLinearInEasing)
            )
            // Ganti konten saat kartu berada pada posisi 90 derajat (tidak terlihat)
            displayedData = stepData
            // Fase 2: Putar balik dari -90 ke 0 derajat (muncul)
            rotationX.snapTo(-90f)
            rotationX.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 160, easing = LinearOutSlowInEasing)
            )
        }
    }

    val statusBorderColor = when {
        displayedData.isPass -> SuccessGreen.copy(alpha = 0.5f)
        displayedData.isLocked -> Color(0xFF1E293B)
        else -> Color(0xFFF59E0B).copy(alpha = 0.5f)
    }

    val statusBgColor = when {
        displayedData.isPass -> Color(0xFF06231A)
        displayedData.isLocked -> Color(0xFF0B132B)
        else -> Color(0xFF261905)
    }

    val statusTextColor = when {
        displayedData.isPass -> SuccessGreen
        displayedData.isLocked -> Color(0xFF64748B)
        else -> Color(0xFFF59E0B)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.rotationX = rotationX.value
                this.cameraDistance = 16f * density
            }
            .clip(RoundedCornerShape(10.dp))
            .background(statusBgColor)
            .border(1.dp, statusBorderColor, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Column {
            // Value status utama dari stepper
            Text(
                text = displayedData.detail,
                color = statusTextColor,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Keterangan penjelasan kuantitatif
            Text(
                text = displayedData.helperDescription,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun StepNodeSlot(
    modifier: Modifier = Modifier,
    stepNumber: String,
    isPass: Boolean,
    isLocked: Boolean,
    isSelected: Boolean,
    hasLeftLine: Boolean,
    hasRightLine: Boolean,
    leftLinePassed: Boolean,
    rightLineProgress: Float,
    rightLinePassed: Boolean
) {
    Box(
        modifier = modifier.height(30.dp),
        contentAlignment = Alignment.Center
    ) {
        // Garis sebelah kiri menyambung ke titik tengah lingkaran
        if (hasLeftLine) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(0.5f)
                    .height(3.5.dp)
                    .background(if (leftLinePassed) SuccessGreen else Color(0xFF1E293B))
            )
        }

        // Garis sebelah kanan menyambung dari titik tengah lingkaran ke slot berikutnya
        if (hasRightLine) {
            val animatedProgress by animateFloatAsState(
                targetValue = if (rightLinePassed) 1f else rightLineProgress.coerceIn(0f, 1f),
                animationSpec = tween(durationMillis = 350),
                label = "Progress$stepNumber"
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxWidth(0.5f)
                    .height(3.5.dp)
                    .background(Color(0xFF1E293B))
            ) {
                if (animatedProgress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress)
                            .background(SuccessGreen)
                    )
                }
            }
        }

        // Lingkaran Angka Stepper berada tepat di atas garis
        val circleBg = when {
            isPass -> SuccessGreen
            isLocked -> Color(0xFF1E293B)
            else -> Color(0xFFF59E0B)
        }
        val circleTextColor = when {
            isPass -> Color.White
            isLocked -> Color(0xFF64748B)
            else -> Color.Black
        }

        val circleBorderColor = when {
            isSelected -> Color.White
            isPass -> SuccessGreen
            isLocked -> Color(0xFF334155)
            else -> Color(0xFFF59E0B)
        }

        Box(
            modifier = Modifier
                .size(if (isSelected) 28.dp else 25.dp)
                .background(circleBg, CircleShape)
                .border(
                    width = if (isSelected) 2.dp else 1.5.dp,
                    color = circleBorderColor,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isPass) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Valid",
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            } else if (isLocked) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Terkunci",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(11.dp)
                )
            } else {
                Text(
                    text = stepNumber,
                    color = circleTextColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun StepTextColumn(
    modifier: Modifier = Modifier,
    title: String,
    isPass: Boolean,
    isLocked: Boolean,
    isSelected: Boolean
) {
    Column(
        modifier = modifier.padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = when {
                isSelected -> Color.White
                isPass -> SuccessGreen
                isLocked -> Color(0xFF64748B)
                else -> Color(0xFFF59E0B)
            },
            fontSize = 11.5.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
            maxLines = 1
        )
    }
}

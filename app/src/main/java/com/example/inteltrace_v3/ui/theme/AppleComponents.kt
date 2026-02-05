package com.example.inteltrace_v3.ui.theme

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Apple Design Language Components
 */

// Spacing constants based on 8pt grid
object AppleSpacing {
    val xsmall = 4.dp
    val small = 8.dp
    val medium = 16.dp
    val large = 24.dp
    val xlarge = 32.dp
    val xxlarge = 40.dp
}

// Corner Radius
object AppleRadius {
    val small = 8.dp      // ~ Nested elements
    val medium = 12.dp    // ~ Standard buttons
    val large = 20.dp     // ~ Cards
    val xlarge = 24.dp    // ~ Large Cards / Modals
}

/**
 * Apple-style Card with soft shadow and large corner radius.
 * Known as "Squircles" in iOS, approximated here with RoundedCornerShape.
 */
@Composable
fun AppleCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = CardBackground,
    contentPadding: Dp = AppleSpacing.medium,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .padding(vertical = 4.dp, horizontal = 0.dp) // Subtle spacing for shadow
            .softShadow(
                color = Color.Black.copy(alpha = 0.05f),
                borderRadius = AppleRadius.large,
                blurRadius = 30.dp,
                offsetY = 10.dp
            )
            .clip(RoundedCornerShape(AppleRadius.large))
            .background(backgroundColor)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(contentPadding)
    ) {
        Column {
            content()
        }
    }
}

/**
 * A modifier to draw a soft, diffused shadow similar to iOS.
 */
fun Modifier.softShadow(
    color: Color = Color.Black.copy(alpha = 0.1f),
    borderRadius: Dp = 0.dp,
    blurRadius: Dp = 10.dp,
    offsetY: Dp = 4.dp,
    offsetX: Dp = 0.dp
) = this.drawBehind {
    this.drawIntoCanvas {
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.color = color.toArgb()
        frameworkPaint.setShadowLayer(
            blurRadius.toPx(),
            offsetX.toPx(),
            offsetY.toPx(),
            color.toArgb()
        )
        it.drawRoundRect(
            0f,
            0f,
            this.size.width,
            this.size.height,
            borderRadius.toPx(),
            borderRadius.toPx(),
            paint
        )
    }
}

/**
 * Apple-style Button: High radius, bold text.
 */
@Composable
fun AppleButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = SystemBlue,
    textColor: Color = Color.White,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(AppleRadius.medium))
            .background(if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.3f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) textColor else textColor.copy(alpha = 0.7f),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
    }
}

/**
 * Glassmorphism-style Header or Panel.
 * Note: Real-time blur on Android Compose needs `RenderEffect` (API 31+) or Glide/Toolkit.
 * Here we simulate it with high translucency.
 */
@Composable
fun GlassyHeader(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp), // Standard header height
        color = SystemBackground.copy(alpha = 0.85f), // Translucent background
        // In a real app, you would apply a blur effect to the background modifier here if minSDK >= 31
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = AppleSpacing.medium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                content = content
            )
            // Add a subtle separator line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.Black.copy(alpha = 0.05f))
                    .align(Alignment.BottomCenter)
            )
        }
    }
}

/**
 * Section Header
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = LabelPrimary
        ),
        modifier = modifier.padding(
            start = AppleSpacing.small,
            bottom = AppleSpacing.small,
            top = AppleSpacing.large
        )
    )
}

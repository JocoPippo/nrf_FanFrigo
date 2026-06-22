package no.nordicsemi.android.blinky.ui.control.view


import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
//import androidx.wear.compose.material.ContentAlpha
import no.nordicsemi.android.blinky.ble.data.int2Float
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin


@Composable
fun TemperatureMeter(
    modifier: Modifier = Modifier,
    minTemp: Int = 0,
    maxTemp: Int = 100,
    temperatureIn: UInt = 0x00001200U,
    enabled: Boolean = true
) {
    var temperature = getMeterValue(temperatureIn)
    var alpha = 1f
    if(!enabled) {
        alpha = 0.3f//ContentAlpha.disabled
        temperature = 0f
    }
    modifier.alpha(alpha=alpha)

    var oldTemperature by remember {
        mutableFloatStateOf(temperature)
    }
    SideEffect {
        oldTemperature = temperature
    }
    val animatedTemperature = remember {
        Animatable(oldTemperature)
    }
    val tempState = animatedTemperature.asState()
    LaunchedEffect(temperature) {
        animatedTemperature.animateTo(
            temperature,
            animationSpec = tween(
                durationMillis = 500,
                easing = EaseInCubic
            )
        )
    }
    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .padding(1.dp)
                .width(150.dp)
                .height(250.dp)
                .alpha(alpha=alpha)
        ) {
            val bigArcRadius = 25.dp.toPx()
            val cutoutDegree = 60f
            val topShapeWidth = cutoutWidth(cutoutDegree, bigArcRadius)
            val smallArcRadius = topShapeWidth / 2
            val columnLeft = (size.width - topShapeWidth) / 2
            val columnRight = columnLeft + topShapeWidth
            val bottomRect = Rect(
                center = Offset(x = size.width / 2, y = size.height - (bigArcRadius)),
                radius = bigArcRadius
            )
/*            drawText(
                textMeasurer = textMeasurer,
                text = "pippo",

                style = TextStyle(
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Left
                ),
            )

 */
            val outlinePath = thermometerOutlinePath(
                leftLineX = columnLeft,
                rightLineX = columnRight,
                smallArcRadius = smallArcRadius,
                cutoutDegree = cutoutDegree,
                bigArcRadius = bigArcRadius
            )
            val fillPath = thermometerFillPath(
                leftLineX = columnLeft,
                rightLineX = columnRight,
                bigArcRadius = bigArcRadius,
                smallArcRadius = smallArcRadius,
                cutoutDegree = cutoutDegree,

                )
            drawThermometer(
                textMeasurer = textMeasurer,
                minTemp = minTemp,
                maxTemp = maxTemp,
                temperature = tempState.value,
                outlinePath = outlinePath,
                fillPath = fillPath,
                columnRight = columnRight,
                bottomRect = bottomRect,
                smallArcRadius = smallArcRadius,
                tempDiv = 5
            )
        }
        if(enabled)
            Text(text = "T=$temperature °C", color = Color.White)
        else
            Text(text = "T=--.-- °C", color = Color.LightGray)
    }


}

fun DrawScope.drawFillPath(
    outlinePath: Path,
    top: Offset = Offset.Zero
) {
    clipPath(
        outlinePath
    ) {
        drawRect(
            color = Color(0xffe01e37),
            topLeft = top
        )
    }
}

private fun getMeterValue(inputPercentage: UInt): Float {
    return if (inputPercentage < 0x00U) {
        0f
    } else if (inputPercentage > 0x00640000U) {
        100f
    } else {
        int2Float(inputPercentage)
    }
}

private fun DrawScope.drawThermometer(
    textMeasurer: TextMeasurer,
    outlinePath: Path,
    fillPath: Path,
    maxTemp: Int,
    tempDiv: Int,
    minTemp: Int,
    temperature: Float,
    columnRight: Float,
    bottomRect: Rect,
    smallArcRadius: Float
) {
    val topOfMeter = Offset(
        x = columnRight + 8.dp.toPx(),
        y = smallArcRadius
    )
    val bottomOfMeter = Offset(
        x = columnRight + 8.dp.toPx(),
        y = bottomRect.top,
    )
    val height = bottomOfMeter.y - topOfMeter.y
    val tempRange = abs(minTemp) + abs(maxTemp)
    val tempUnits = tempRange / tempDiv
    val unitHeight = height / tempUnits
    drawFillPath(
        outlinePath = fillPath,
        top = Offset(
            0f,
            topOfMeter.y + (unitHeight) * (maxTemp - temperature) / tempDiv
        )
    )

    drawThermometerPath(
        outlinePath,
        strokeWidth = 2.dp
    )
    drawRuler(
        top = topOfMeter,
        bottom = bottomOfMeter,
        textMeasurer = textMeasurer,
        minTemp = minTemp,
        maxTemp = maxTemp,
        tempDiv = tempDiv
    )


}


private fun DrawScope.drawRuler(
    minTemp: Int = -0,
    maxTemp: Int = 100,
    tempDiv: Int = 2,
    verticalLineColor: Color = Color.LightGray,
    verticalLineWidth: Dp = 2.dp,
    horizontalLineHeight: Dp = 2.dp,
    horizontalLineColor: Color = Color.LightGray,
    top: Offset,
    bottom: Offset,
    textMeasurer: TextMeasurer,
    textColor: Color = Color.LightGray,
    textSize: TextUnit = 13.sp
) {

    val height = (bottom.y - top.y)
    val tempRange = abs(minTemp) + abs(maxTemp)
    val tempUnits = tempRange / tempDiv
    for (step in 0..tempUnits) {
        val length = if (step % 5 == 0) 8.dp else 4.dp
        drawLine(
            color = horizontalLineColor,
            start = bottom - Offset(x = 0f, y = (step * (height / tempUnits))),
            end = bottom - Offset(x = -length.toPx(), y = (step * (height / tempUnits))
            ),
            strokeWidth = horizontalLineHeight.toPx(),
            cap = StrokeCap.Round
        )
        if (step % 5 == 0 || step == tempUnits) drawText(
            textMeasurer = textMeasurer,
            text = ((step * tempDiv) + minTemp).toString(),
            topLeft = bottom - Offset(
                x = -15.dp.toPx(), y = (step * (height / tempUnits)) + 8.dp.toPx()
            ),
            style = TextStyle(
                color = textColor,
                fontSize = textSize,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Left
            ),
            size = Size(25.dp.toPx(), 20.dp.toPx())
        )

    }
    drawLine(
        color = verticalLineColor,
        start = top,
        end = bottom,
        cap = StrokeCap.Round,
        strokeWidth = verticalLineWidth.toPx()
    )


}

fun DrawScope.thermometerFillPath(
    leftLineX: Float,
    rightLineX: Float,
    smallArcRadius: Float,
    cutoutDegree: Float,
    bigArcRadius: Float,
    top: Float = 0f,
    padding: Dp = 5.dp
): Path {
    return Path().apply {
        arcTo(
            rect = Rect(
                left = leftLineX + padding.toPx(),
                top = top + padding.toPx(),
                right = rightLineX - padding.toPx(),
                bottom = (smallArcRadius * 2) - padding.toPx()
            ), startAngleDegrees = -180f, 180f, false
        )

        arcTo(
            rect = Rect(
                center = Offset(
                    x = (size.width / 2),
                    y = size.height - (bigArcRadius)
                ),
                radius = bigArcRadius - padding.toPx()
            ),
            forceMoveTo = false,
            startAngleDegrees = -90 + ((cutoutDegree - padding.toPx()) / 2),
            sweepAngleDegrees = 360 - cutoutDegree + padding.toPx()

        )
        lineTo(x = leftLineX + padding.toPx(), y = smallArcRadius + padding.toPx())

    }

}

fun DrawScope.thermometerOutlinePath(
    leftLineX: Float,
    rightLineX: Float,
    smallArcRadius: Float,
    cutoutDegree: Float,
    bigArcRadius: Float,
): Path {
    return Path().apply {
        arcTo(
            rect = Rect(
                left = leftLineX, top = 0f, right = rightLineX, bottom = smallArcRadius * 2
            ), startAngleDegrees = -180f, 180f, false
        )

        arcTo(
            rect = Rect(
                center = Offset(x = size.width / 2, y = size.height - (bigArcRadius)),
                radius = bigArcRadius
            ),
            forceMoveTo = false,
            startAngleDegrees = -90 + (cutoutDegree / 2),
            sweepAngleDegrees = 360 - cutoutDegree

        )
        lineTo(x = leftLineX, y = smallArcRadius)

    }

}

fun DrawScope.drawThermometerPath(
    path: Path,
    strokeWidth: Dp = Dp.Hairline
) {
    drawPath(
        path = path, Color.LightGray, style = Stroke(
            cap = StrokeCap.Round,
            width = strokeWidth.toPx()
        )
    )
}

private fun cutoutWidth(arcDegree: Float, radius: Float) =
    2 * (sin((arcDegree / 2) * PI / 180).toFloat() * radius)


@Preview
@Composable
private fun TemperatureComponentPreview() {

        TemperatureMeter(
            temperatureIn = 0x00301200U,
            enabled = false

        )

}
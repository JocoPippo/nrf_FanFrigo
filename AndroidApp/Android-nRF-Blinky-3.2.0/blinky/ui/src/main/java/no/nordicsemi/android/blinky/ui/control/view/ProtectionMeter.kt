package no.nordicsemi.android.blinky.ui.control.view


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
//import androidx.wear.compose.material.ContentAlpha
import no.nordicsemi.android.blinky.ble.data.int2Float
import kotlin.math.cos
import kotlin.math.sin


@Composable
fun ProtectionMeter(
    modifier: Modifier = Modifier,
    inputValue: UInt,
    trackColor: Color = Color.DarkGray,
    progressColors: List<Color>,
    innerGradient: Color,
    percentageColor: Color = Color.White,
    measureUnit: String = "°C",
    tagText: String ="Temperature",
    enabled : Boolean = true

) {

    var meterValue = getMeterValue(inputValue)
    var alpha = 1f
    if(!enabled) {
        alpha = 0.3f//ContentAlpha.disabled
        meterValue = 0f
    }
    modifier.alpha(alpha=alpha)
    Box(modifier = modifier.size(196.dp).alpha(alpha)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sweepAngle = 240f
            val fillSwipeAngle = (meterValue / 100f) * sweepAngle
            val height = size.height
            val width = size.width
            val startAngle = 150f
            val arcHeight = height - 20.dp.toPx()

            drawArc(
                color = trackColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset((width - height + 60f) / 2f, (height - arcHeight) / 2f),
                size = Size(arcHeight, arcHeight),
                style = Stroke(width = 50f, cap = StrokeCap.Round)
            )

            drawArc(
                brush = Brush.horizontalGradient(progressColors),
                startAngle = startAngle,
                sweepAngle = fillSwipeAngle,
                useCenter = false,
                topLeft = Offset((width - height + 60f) / 2f, (height - arcHeight) / 2),
                size = Size(arcHeight, arcHeight),
                style = Stroke(width = 50f, cap = StrokeCap.Round)
            )
            val centerOffset = Offset(width / 2f, height / 2.09f)
            drawCircle(
                Brush.radialGradient(
                    listOf(
                        innerGradient.copy(alpha = 0.2f),
                        Color.Transparent
                    )
                ), width / 2f
            )
            drawCircle(Color.White, 24f, centerOffset)

            // Calculate needle angle based on inputValue
            val needleAngle = (meterValue / 100f) * sweepAngle + startAngle
            val needleLength = 240f // Adjust this value to control needle length
            val needleBaseWidth = 10f // Adjust this value to control the base width


            val needlePath = Path().apply {
                // Calculate the top point of the needle
                val topX = centerOffset.x + needleLength * cos(
                    Math.toRadians(needleAngle.toDouble()).toFloat()
                )
                val topY = centerOffset.y + needleLength * sin(
                    Math.toRadians(needleAngle.toDouble()).toFloat()
                )

                // Calculate the base points of the needle
                val baseLeftX = centerOffset.x + needleBaseWidth * cos(
                    Math.toRadians((needleAngle - 90).toDouble()).toFloat()
                )
                val baseLeftY = centerOffset.y + needleBaseWidth * sin(
                    Math.toRadians((needleAngle - 90).toDouble()).toFloat()
                )
                val baseRightX = centerOffset.x + needleBaseWidth * cos(
                    Math.toRadians((needleAngle + 90).toDouble()).toFloat()
                )
                val baseRightY = centerOffset.y + needleBaseWidth * sin(
                    Math.toRadians((needleAngle + 90).toDouble()).toFloat()
                )

                moveTo(topX, topY)
                lineTo(baseLeftX, baseLeftY)
                lineTo(baseRightX, baseRightY)
                close()
            }

            drawPath(
                color = Color.White,
                path = needlePath
            )
        }

        Column(
            modifier = Modifier
                .padding(bottom = 5.dp)
                .align(Alignment.BottomCenter), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if(enabled) {
                Text(
                    text = "$meterValue $measureUnit",
                    fontSize = 20.sp,
                    lineHeight = 28.sp,
                    color = percentageColor
                )
                //Text(text = "Temperature", fontSize = 16.sp, lineHeight = 24.sp, color = Color(0xFFB0B4CD))
                Text(text = tagText, fontSize = 16.sp, lineHeight = 24.sp, color = percentageColor)
            }
            else {
                Text(
                    text = "--.-- $measureUnit",
                    fontSize = 20.sp,
                    lineHeight = 28.sp,
                    color = percentageColor
                )
                //Text(text = "Temperature", fontSize = 16.sp, lineHeight = 24.sp, color = Color(0xFFB0B4CD))
                Text(text = tagText, fontSize = 16.sp, lineHeight = 24.sp, color = percentageColor)

            }
        }

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

@Composable
@Preview
private fun ProtectionMeterViewPreview() {
    ProtectionMeter(
        inputValue =  0x00001a68U,
        innerGradient = Color.Yellow,
        progressColors = listOf(Color.Green,Color.Red),
        enabled = false
        //trackColor =Color.DarkGray
    )
}
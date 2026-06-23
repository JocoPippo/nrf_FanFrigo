package no.nordicsemi.android.blinky.ui.control.view


import android.R.attr.value
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
internal fun SliderControlView(
    value: Float,
    setValue: (Float) -> Unit,
    enabled: Boolean,
    unit: String,
    modifier: Modifier = Modifier,
    height: Dp,

    ) {
    var slidepos by remember { mutableFloatStateOf(30f) }
    var alpha = 1f
    if(!enabled) {
        alpha = 0.3f//ContentAlpha.disabled
    }
    modifier.alpha(alpha=alpha)

    Row(
        modifier = modifier,
        horizontalArrangement =  Arrangement.Start

    )
    {
        //slidepos = int2Float(value )
        //val state = SliderState(value = slidepos, valueRange = 0f..100f)
        Slider(
            //state = state,
            value = slidepos,
            onValueChange = setValue,
            valueRange = 0f..100f,
            /*modifier = modifier.clickable {
                setValue(
                    setForced4Int(
                        float2Int(slidepos),
                        isForced = true
                    )
                )
            }*/
            modifier = modifier.height(height),
            enabled = enabled,
            //reverseDirection = true
        )
        Text(
            text = (slidepos).toString() + unit,
            color = Color.White,
            fontSize = 20.sp,
            modifier = modifier//.alpha(alpha = alpha)
        )
    }
}

@Composable
fun VerticalSlider(
    sliderpos: Float,
    onValueChange: (Float) -> Unit,
    unit: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,

    valueRange: ClosedFloatingPointRange<Float> = 0f..100f,
    /*@IntRange(from = 0)*/
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: SliderColors = SliderDefaults.colors(),
    height: Int,
) {
    var value by remember { mutableFloatStateOf(sliderpos)}
    Slider(
        colors = colors,
        interactionSource = interactionSource,
        onValueChangeFinished = {onValueChange(value)},//onValueChangeFinished,
        steps = steps,
        valueRange = valueRange,
        enabled = enabled,
        //value = sliderpos,
        value = value,
        onValueChange = { it->
            value=it
            //onValueChange(value)
            },
        modifier = Modifier
            .graphicsLayer {
                rotationZ = 270f
                transformOrigin = TransformOrigin(0f, 0f)
            }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(
                    Constraints(
                        /*
                        minWidth = constraints.minHeight,
                        maxWidth = constraints.maxHeight,
                        minHeight = constraints.minWidth,
                        maxHeight = constraints.maxHeight,

                         */
                        minWidth = constraints.minHeight,
                        maxWidth = height,
                        minHeight = constraints.minWidth,
                        maxHeight = constraints.maxHeight,

                        )
                )
                layout(placeable.height, placeable.width) {
                    placeable.place(-placeable.width, 0)
                }
            }
            .then(modifier)
    )
    Text(
        text = "$sliderpos $unit", color = Color.White, fontSize = 20.sp)
}


@Composable
@Preview
private fun SliderControlViewPreview() {
    /*
    SliderControlView(
        value = 33.20f,
        setValue = {},
        enabled = true,
        unit = "°C",
        modifier = Modifier,
        height = 150.dp
    )

     */
    VerticalSlider(sliderpos = 54.03f, unit = "°C", onValueChange = {}, height = 450)
    Spacer(modifier = Modifier.height(100.dp))
    VerticalSlider(sliderpos = 54.03f, unit = "°C", onValueChange = {}, height = 650)
}

/*
@Composable
@Preview
private fun SliderControlViewPreview() {
    VerticalSlider(sliderpos = 54.03f, unit = "°C", onValueChange = {})
}
*/

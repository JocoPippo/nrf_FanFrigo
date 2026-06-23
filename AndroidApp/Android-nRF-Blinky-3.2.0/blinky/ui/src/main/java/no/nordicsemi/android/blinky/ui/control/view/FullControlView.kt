package no.nordicsemi.android.blinky.ui.control.view


import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.blinky.ble.data.int2Float
import no.nordicsemi.android.blinky.ui.R


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FullControlView(
    dutyValue: UInt,
    tempValue: UInt,
    setDutyCycle: (UInt) -> Unit,
    modifier: Modifier = Modifier,
) {
    var slidepos by remember { mutableFloatStateOf(50f) }
    var checked by remember { mutableStateOf(false) }
    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    OutlinedCard(
        modifier = modifier

    ) {
        Column(
            modifier = Modifier
                .clickable { setDutyCycle(slidepos.toUInt()) }
                .padding(8.dp)
                .onSizeChanged{ cardSize = it}
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.blinky_dutyCycle),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = (dutyValue).toString() +"%")
                slidepos = int2Float(dutyValue)
                Slider(value = slidepos,
                    //onValueChange = {if(!checked)slidepos = it},
                    onValueChange = {slidepos = it},
                    valueRange = 10f..100f, //steps = 100,
                    //enabled = checked,
                    interactionSource = interactionSource,
                    /*
                    thumb = {
                            Text(text = slidepos.toString(), color = Color.Black)
                    }
                    */

                    )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.blinky_dutyCycleUpdate),
                    //style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.size(size=5.dp) )
                Switch(checked = checked,{checked = it})
//                IconButton({setDutyCycle(slidepos.toInt())}, modifier) {
//                    Icon( imageVector = Icons.Default.ArrowCircleUp, contentDescription = "update" )
//                }
                Spacer(Modifier.size(width = (cardSize.width.dp-800.dp), height = 1.dp ))
                ElevatedButton(onClick = {setDutyCycle(slidepos.toUInt())}, enabled = checked) {
                    Text("Apply")}
            }
        }
    }
/*
fun enableUpdate(checked: Boolean) {
    if(checked==true) {
        ElevatedButton
    }
}
*/
}

@Composable
@Preview
private fun DutyCycleControlViewPreview() {
    FullControlView(
        dutyValue = 0x00320000U,
        tempValue = 0x00380000U,
        setDutyCycle = {},
        modifier = Modifier.padding(16.dp),
    )
}
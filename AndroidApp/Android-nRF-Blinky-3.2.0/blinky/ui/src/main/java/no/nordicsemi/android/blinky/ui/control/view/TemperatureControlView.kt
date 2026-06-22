package no.nordicsemi.android.blinky.ui.control.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.blinky.ble.fromUInt2Float
import no.nordicsemi.android.blinky.ui.R

@Composable
internal fun TemperatureControlView(
    tempValue: UInt,
    thresholdValue: UInt,
    forcedState: Boolean,
    modifier: Modifier = Modifier,
) {
    var temp by remember { mutableStateOf(0x00120001U) }
    var threshold by remember { mutableStateOf(0x00300002U) }
    var forced by remember { mutableStateOf(false) }
    forced = forcedState
    temp = tempValue
    threshold = thresholdValue
    OutlinedCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    imageVector = Icons.Default.DeviceThermostat,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 16.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                )
                Text(
                    text = stringResource(R.string.camperFan_temp),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp).padding(top=3.dp),//.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = stringResource(R.string.camperFan_temp_descr),
                    modifier = Modifier.weight(1f))
                OutlinedTextField(
                    value = fromUInt2Float(temp).toString(),
                    readOnly = true,
//                    textStyle = MaterialTheme.typography.headlineSmall,
                    //labelPosition = TextFieldLabelPosition.Above(),
                    label = {  Text(
                        text = stringResource(R.string.camperFan_temp),
                    )},
                    onValueChange = { null },

                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp).padding(top=3.dp),

                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.camperFan_threshold_descr),
                    modifier = Modifier.weight(1f)
                )

                    OutlinedTextField(
                        readOnly = true,
                        value = fromUInt2Float(threshold).toString(),
//                        textStyle = MaterialTheme.typography.headlineSmall,
                        label = {  Text(
                            text = stringResource(R.string.camperFan_threshold),
                        )},
                        onValueChange = { null },

//                        labelPosition = TextFieldLabelPosition.Attached(),
//                        state = rememberTextFieldState(),
//                        label = { Text(text = fromUInt2Float(threshold).toString(),
//                            style = MaterialTheme.typography.headlineSmall,
//
//                        )},
                        modifier = Modifier.weight(1f)
                    )
            }

            // Legend.
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    //if(isForced(threshold)) {
                    if(forced) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp).size(12.dp)
                        )
                        Text(
                            text = stringResource(
                                R.string.camperFan_forcedOn,
                            ).uppercase(),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    else {
                        Icon(
                            imageVector = Icons.Default.AutoMode,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp).size(12.dp)
                        )
                        Text(
                            text = stringResource(
                                R.string.camperFan_forcedOff
                            ).uppercase(),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
@Preview
private fun TemperatureControlViewPreview() {
    TemperatureControlView(
        tempValue = 0x00140004U,
        thresholdValue = 0x802b0005U,
        forcedState = false,
        modifier = Modifier.padding(16.dp),
    )
}
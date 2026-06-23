package no.nordicsemi.android.blinky.ui.control.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.twotone.AutoMode
import androidx.compose.material.icons.twotone.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
internal fun DutCycleControlView(
    dutyCycleValue: UInt,
    forcedState: Boolean,
    modifier: Modifier = Modifier,
) {
    var duty by remember { mutableStateOf(0x00120001U) }
    var forced by remember { mutableStateOf(false) }
    forced = forcedState
    duty = dutyCycleValue

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
                    imageVector = Icons.Default.AcUnit,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 16.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                )
                Text(
                    text = stringResource(R.string.camperFan_duty),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),//.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.camperFan_duty_descr),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = fromUInt2Float(duty).toString(),
                    readOnly = true,
//                    textStyle = MaterialTheme.typography.headlineSmall,
                    label = {  Text(
                        text = stringResource(R.string.camperFan_duty),
                    )},
                    onValueChange = { null },
                    modifier = Modifier.weight(1f)

                )
            }

            // Legend.
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onSurface.copy(
                    alpha = 0.8f
                )
            ) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    //if(isForced(threshold)) {
                    if (forced) {
                        Icon(
                            imageVector = Icons.TwoTone.Lock,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(12.dp)
                        )
                        Text(
                            text = stringResource(
                                R.string.camperFan_forcedOn,
                            ).uppercase(),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.TwoTone.AutoMode,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(12.dp)
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
private fun DutCycleControlViewPreview() {
    DutCycleControlView(
        dutyCycleValue = 0x000A0008U,
        forcedState = false,
        modifier = Modifier.padding(16.dp),
    )
}
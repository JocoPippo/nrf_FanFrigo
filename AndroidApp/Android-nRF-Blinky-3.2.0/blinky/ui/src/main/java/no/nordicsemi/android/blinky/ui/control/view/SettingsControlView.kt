package no.nordicsemi.android.blinky.ui.control.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.twotone.Air
import androidx.compose.material.icons.twotone.Cyclone
import androidx.compose.material.icons.twotone.DeviceThermostat
import androidx.compose.material.icons.twotone.FlashOff
import androidx.compose.material.icons.twotone.FlashOn
import androidx.compose.material.icons.twotone.ModeFanOff
import androidx.compose.material.icons.twotone.Power
import androidx.compose.material.icons.twotone.PowerOff
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedIconToggleButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import no.nordicsemi.android.blinky.ble.fromUInt2Float
import no.nordicsemi.android.blinky.ui.R
import java.util.regex.Pattern


class SettingsViewModel : ViewModel() {

    val FLOAT_NUMBER_STRING: String = ("^([0-9]{0,2}([.][0-9]{0,2})?|[.][0-9]{0,2}|100([.][0]{0,2})?)$")
    val FLOAT_NUMBER: Pattern = Pattern.compile(FLOAT_NUMBER_STRING)
    var duty by mutableStateOf("1")
        private set

    val dutyHasErrors by derivedStateOf {
        if (duty.isNotEmpty()) {

            !FLOAT_NUMBER.matcher(duty).matches()

        } else {
            true
        }
    }

    fun updateDuty(input: String) {
        duty = input
    }
}



@Composable
internal fun SettingsControlView(

    dutyCycleSetPoint: UInt,
    dutyCycleForce: Boolean,
    thresholdSetPoint: UInt,
    thresholdForce: Boolean,
    fan2State: Boolean,
    fan2Force: Boolean,

    powerOnState: Boolean,
    ledsOnState: Boolean,
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel,
    powerOnCallback : (Boolean) -> Unit,
    dutySetCallback:  (Boolean, String) -> Unit,
    thresholdCallback: (Boolean, String) -> Unit,
    fanCallback: (Boolean, Boolean) -> Unit,
    ledsOnCallback : (Boolean) -> Unit,
    ) {
    var dutyCycleSet by remember { mutableStateOf(0x00120001U) }
    var dutyCycleForced by remember { mutableStateOf(false) }
    var thresholdSet by remember { mutableStateOf(0x00120001U) }
    var thresholdForced by remember { mutableStateOf(false) }
    //var fan2 by remember { mutableStateOf(0.toUByte()) }
    var fan2 by remember { mutableStateOf(false) }
    var fan2Forced by remember { mutableStateOf(false) }
    var powerOn by remember { mutableStateOf(false) }
    var ledsState by remember { mutableStateOf(false) }

/*
    dutyCycleSet = dutyCycleSetPoint
    dutyCycleForced = isForced(dutyCycleSet)
    thresholdSet = thresholdSetPoint
    thresholdForced = isForced(thresholdSet)
    fan2 = purgeForced(fan2State) == 0x01.toUByte()
    fan2Forced = isForced(fan2State)
*/
    dutyCycleSet = dutyCycleSetPoint
    dutyCycleForced = dutyCycleForce
    thresholdSet = thresholdSetPoint
    thresholdForced = thresholdForce
    fan2 = fan2State
    fan2Forced = fan2Force


    powerOn = powerOnState
    ledsState = ledsOnState

    //var duty = fromUInt2Float(0x0001000bU).toString()
    var duty by remember { mutableStateOf(fromUInt2Float(dutyCycleSet).toString()) }
    var threshold by remember { mutableStateOf(fromUInt2Float(thresholdSet).toString()) }

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
                    imageVector = Icons.TwoTone.Settings,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 16.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                )
                Text(
                    text = stringResource(R.string.camperFan_settings),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(.3f),
                )
            }
//Power On
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .align(Alignment.Start),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {

                OutlinedIconToggleButton(
                    checked = powerOn,
                    onCheckedChange = {
                        powerOn = it
                        powerOnCallback(powerOn)
                    },
                    border = if (powerOn) null else BorderStroke(1.dp, DividerDefaults.color),
                    modifier = Modifier.weight(.3f),
                    shape = IconButtonDefaults.filledShape,
                    colors = IconButtonDefaults.iconToggleButtonColors(
                        containerColor = Color.Red,
                        contentColor = Color.Black,
                        checkedContainerColor = Color.Green,
                        checkedContentColor = Color.Yellow
                    ),
                ) {
                    Icon(
                        imageVector = if (powerOn) Icons.TwoTone.Power else Icons.TwoTone.PowerOff,
                        contentDescription = null,
                    )
                }
                Spacer(modifier = Modifier.weight(.2f))
                OutlinedIconToggleButton(
                    checked = ledsState,
                    onCheckedChange = {
                        ledsState = it
                        ledsOnCallback(ledsState)
                    },
                    border = if (ledsState) null else BorderStroke(1.dp, DividerDefaults.color),
                    modifier = Modifier.weight(.3f),
                    shape = IconButtonDefaults.filledShape,
                    colors = IconButtonDefaults.iconToggleButtonColors(
                        containerColor = Color.Gray,
                        contentColor = Color.Black,
                        checkedContainerColor = Color.Blue,
                        checkedContentColor = Color.Yellow
                    ),
                ) {
                    Icon(
                        imageVector = if (ledsState) Icons.TwoTone.FlashOn else Icons.TwoTone.FlashOff,
                        contentDescription = "Leds",
                    )
                }

                //Spacer(modifier = Modifier.weight(.1f))
            }
// DutyCycle Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                OutlinedTextField(
                    //value = fromUInt2Float(dutyCycleSet).toString(),
                    value = duty,
                    readOnly = !dutyCycleForced,
//                    textStyle = MaterialTheme.typography.headlineSmall,
                    label = {
                        Text(
                            text = stringResource(R.string.camperFan_duty_descr),
                        )
                    },
                    onValueChange = { input ->
                        duty = input
                        settingsViewModel.updateDuty(input)
                                    },
                    isError = settingsViewModel.dutyHasErrors,
                    supportingText = {
                        if (settingsViewModel.dutyHasErrors) {
                            Text("Incorrect number format.")
                        }
                    },
                    modifier = Modifier
                        .weight(.3f)
                        .padding(0.dp),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.TwoTone.Air,
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(0.dp),
                        )
                    },
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(.2f)
                        .heightIn(min = 48.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Checkbox(
                        // set the state of checkbox.
                        checked = dutyCycleForced,
                        onCheckedChange = { dutyCycleForced = it },
                        modifier = Modifier.padding(start = 20.dp),
                        enabled = true,
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color.Red,
                            uncheckedColor = Color.DarkGray,
                            checkmarkColor = Color.White
                        ),
                        interactionSource = remember { MutableInteractionSource() }
                    )
                    Text(
                        text = if (dutyCycleForced) "Lock" else "Free",
                        modifier = Modifier.padding(start = 28.dp)
                    )
                }
                OutlinedButton(onClick = { dutySetCallback(dutyCycleForced, duty) }) {
                    Icon(
                        imageVector = Icons.Outlined.Upload,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 2.dp)
                            .size(26.dp)//.weight(.1f)
                    )
                    Text(
                        "Set",
                    )
                }
            }


//Threshold Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                OutlinedTextField(
                    value = threshold,
                    readOnly = !thresholdForced,
//                    textStyle = MaterialTheme.typography.headlineSmall,
                    label = {
                        Text(
                            text = stringResource(R.string.camperFan_threshold_set),
                        )
                    },
                    //onValueChange = { input -> settingsViewModel.updateDuty(input) },
                    onValueChange = { input ->
                        threshold = input
                        settingsViewModel.updateDuty(input)
                    },
                    isError = settingsViewModel.dutyHasErrors,
                    supportingText = {
                        if (settingsViewModel.dutyHasErrors) {
                            Text("Incorrect number format.")
                        }
                    },
                    modifier = Modifier
                        .weight(.3f)
                        .padding(0.dp),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.TwoTone.DeviceThermostat,
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(0.dp),
                        )
                    },
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(.2f)
                        .heightIn(min = 48.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Checkbox(
                        // set the state of checkbox.
                        checked = thresholdForced,
                        onCheckedChange = { thresholdForced = it },
                        modifier = Modifier.padding(start = 20.dp),
                        enabled = true,
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color.Red,
                            uncheckedColor = Color.DarkGray,
                            checkmarkColor = Color.White
                        ),
                        interactionSource = remember { MutableInteractionSource() }
                    )
                    Text(
                        text = if (thresholdForced) "Lock" else "Free",
                        modifier = Modifier.padding(start = 28.dp)
                    )
                }
                OutlinedButton(onClick = { thresholdCallback(thresholdForced, threshold) }) {
                    Icon(
                        imageVector = Icons.Outlined.Upload,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 2.dp)
                            .size(26.dp)//.weight(.1f)
                    )
                    Text(
                        "Set",
                    )
                }
            }


//Fan2 Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),//.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedIconToggleButton(
                    //checked = fan2 != 0.toUByte(),
                    checked = fan2,
                    onCheckedChange = {
                        //fan2 = if(it) 1.toUByte() else 0.toUByte()
                        fan2 = it
                    },
                    enabled = fan2Forced,
                    //border = if (fan2 != 0.toUByte()) null else BorderStroke(1.dp, DividerDefaults.color),
                    modifier = Modifier.weight(.3f),
                    shape = IconButtonDefaults.filledShape,
                    colors = IconButtonDefaults.iconToggleButtonColors(
                        containerColor = Color.Cyan,
                        contentColor = Color.Black,
                        disabledContainerColor = Color.Gray,
                        disabledContentColor = Color.White,
                        checkedContainerColor = Color.Blue,
                        checkedContentColor = Color.Yellow
                    ),
                ) {
                    Icon(
                        //imageVector = if (fan2 != 0.toUByte()) Icons.TwoTone.Cyclone else Icons.TwoTone.ModeFanOff,
                        imageVector = if (fan2) Icons.TwoTone.Cyclone else Icons.TwoTone.ModeFanOff,
                        contentDescription = null,
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(.2f)
                        .heightIn(min = 48.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Checkbox(
                        // set the state of checkbox.
                        checked = fan2Forced,
                        onCheckedChange = { fan2Forced = it },
                        modifier = Modifier.padding(start = 20.dp),
                        enabled = true,
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color.Red,
                            uncheckedColor = Color.DarkGray,
                            checkmarkColor = Color.White
                        ),
                        interactionSource = remember { MutableInteractionSource() }
                    )
                    Text(
                        text = if (fan2Forced) "Lock" else "Free",
                        modifier = Modifier.padding(start = 28.dp)
                    )
                }
                OutlinedButton(onClick = { fanCallback(fan2Forced, fan2) }) {
                    Icon(
                        imageVector = Icons.Outlined.Upload,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 2.dp)
                            .size(26.dp)//.weight(.1f)
                    )
                    Text(
                        "Set",
                    )
                }
            }
        }
    }
}

@Composable
@Preview
private fun SettingsControlViewPreview() {
    val dutyViewModel: SettingsViewModel = viewModel<SettingsViewModel>()
    SettingsControlView(
        dutyCycleSetPoint = 0x000A0008U,
        dutyCycleForce = false,
        thresholdSetPoint = 0x000B0009U,
        thresholdForce = false,
        fan2State = true,
        fan2Force = false,
        powerOnState = true,
        modifier = Modifier.padding(16.dp),
        settingsViewModel = dutyViewModel,
        powerOnCallback = {},
        dutySetCallback = { _:Boolean, _:String -> },
        thresholdCallback = {_:Boolean, _:String -> },
        //fanCallback = {_:Boolean, _:UByte -> }
        fanCallback = {_:Boolean, _: Boolean -> },
        ledsOnCallback = {},
        ledsOnState = false,
    )
}



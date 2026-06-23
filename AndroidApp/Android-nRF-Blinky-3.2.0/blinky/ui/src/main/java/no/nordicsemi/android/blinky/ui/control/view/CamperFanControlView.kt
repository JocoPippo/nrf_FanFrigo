package no.nordicsemi.android.blinky.ui.control.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import no.nordicsemi.android.blinky.ble.isForced
import no.nordicsemi.android.blinky.ble.purgeForced

//import androidx.wear.compose.material.ContentAlpha

@Composable
internal fun CamperFanControlView(

    temperatureValue: UInt,
    thresholdValue: UInt,
    dutyCycleValue: UInt,
    fan2Value: UByte,
    powerOnState: Boolean,
    onPowerOnChanged: (Boolean) -> Unit,
    dutyUnused: UInt,
    ledsState: Boolean,
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel,
    dutySetCallback : ( Boolean, String) -> Unit,
    thresholdCallback :  ( Boolean, String) -> Unit,
    //fanCallback : (Boolean, UByte) -> Unit,
    fanCallback : (Boolean, Boolean) -> Unit,
    ledsChanged: (Boolean) -> Unit,
//    pswCallback: (Boolean, Int) -> Unit,
//    pswViewModel: PasswordViewModel,

    ) {
    var alpha = 1f
    if (!powerOnState) {
        alpha = 0.3f//ContentAlpha.disabled
    }
    Column(
        modifier = Modifier
            .padding(2.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Start
        ) {

            TemperatureControlView(
                tempValue = temperatureValue,
                thresholdValue = thresholdValue,
                forcedState = isForced(thresholdValue),
                modifier = modifier
            )
        }
        Row(
            horizontalArrangement = Arrangement.Start
        ) {
            DutCycleControlView(
                dutyCycleValue = dutyCycleValue,
                forcedState = isForced(dutyCycleValue),
                modifier = modifier
            )
        }

        Row(
            horizontalArrangement = Arrangement.Start
        ) {
            SettingsControlView(
                //dutyCycleSetPoint = dutyCycleValue,
                dutyCycleSetPoint = purgeForced(dutyUnused),
                dutyCycleForce = isForced(dutyUnused),
                thresholdSetPoint = purgeForced(thresholdValue),
                thresholdForce = isForced(thresholdValue),
                fan2State = purgeForced(fan2Value) == 0x01.toUByte(),
                fan2Force = isForced(fan2Value),
                powerOnState= powerOnState,
                modifier = modifier,
                settingsViewModel = settingsViewModel,
                powerOnCallback = onPowerOnChanged,
                dutySetCallback = dutySetCallback,
                thresholdCallback = thresholdCallback,
                fanCallback = fanCallback,
                ledsOnCallback = ledsChanged,
                ledsOnState = ledsState,
            )
        }
    }
}

@Preview
@Composable
private fun CamperFanControlViewPreview() {
    val dutyViewModel: SettingsViewModel = viewModel<SettingsViewModel>()
 //   val pswViewModel: PasswordViewModel = viewModel< PasswordViewModel>()
    CamperFanControlView(
        temperatureValue = 0x00220061U,
        thresholdValue = 0x002a0000U,
        dutyCycleValue = 0x00400000U,
        fan2Value = 0x01.toUByte(),
        powerOnState = true,
        ledsState = true,
        onPowerOnChanged = {},
//        dutyCycleChanged = {},
//        fan2Changed = {},
//        thresholdChanged ={},
        dutyUnused = 0x00300002U,
        modifier = Modifier,
        settingsViewModel = dutyViewModel,
        dutySetCallback = { _:Boolean, _:String -> },
        thresholdCallback = {_:Boolean, _:String -> },
        //fanCallback = {_:Boolean, _:UByte -> }
        fanCallback = {_:Boolean, _: Boolean -> },
        ledsChanged = {},
//        pswCallback =  { _:Boolean, _:Int -> },
//        pswViewModel = pswViewModel
    )
}
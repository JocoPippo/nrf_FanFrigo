package no.nordicsemi.android.blinky.ui.control.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import no.nordicsemi.android.blinky.spec.CamperFan
import no.nordicsemi.android.blinky.ui.NavigationBarExample
import no.nordicsemi.android.blinky.ui.NavigationTabExample
import no.nordicsemi.android.blinky.ui.R
import no.nordicsemi.android.blinky.ui.control.viewmodel.CamperFanViewModel
import no.nordicsemi.android.common.logger.view.LoggerAppBarIcon
import no.nordicsemi.android.common.permissions.ble.RequireBluetooth
import no.nordicsemi.android.common.ui.view.NordicAppBar
import no.nordicsemi.android.scanner.view.DeviceConnectingView
import no.nordicsemi.android.scanner.view.DeviceDisconnectedView
import no.nordicsemi.android.scanner.view.Reason

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CamperFanScreen(
    onNavigateUp: () -> Unit,
) {
    val viewModel: CamperFanViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        NordicAppBar(
            title = { Text(text = viewModel.deviceName) },
            onNavigationButtonClick = onNavigateUp,
            actions = {
                LoggerAppBarIcon(onClick = { viewModel.openLogger() })
            }
        )
        RequireBluetooth {
            when (state) {
                CamperFan.State.LOADING -> {
                    DeviceConnectingView(
                        modifier = Modifier.padding(16.dp),
                    ) { padding ->
                        Button(
                            onClick = onNavigateUp,
                            modifier = Modifier.padding(padding),
                        ) {
                            Text(text = stringResource(id = R.string.action_cancel))
                        }
                    }
                }
                CamperFan.State.READY -> {
                    /*
                    val temperatureValue by viewModel.temperatureValue.collectAsStateWithLifecycle()
                    val thresholdValue by viewModel.thresholdValue.collectAsStateWithLifecycle()
                    val dutycleValue by viewModel.dutyCycleValue.collectAsStateWithLifecycle()
                    val fan2Value by viewModel.fan2Value.collectAsStateWithLifecycle()
                    val powerOnState by viewModel.powerOnState.collectAsStateWithLifecycle()
                    val ledsState by viewModel.ledsOnState.collectAsStateWithLifecycle()
                    //LPP TODO modify
                    val dutyViewModel: SettingsViewModel = viewModel<SettingsViewModel>()
                    val pswViewModel: PasswordViewModel = viewModel<PasswordViewModel>()
                    //var dutyUnused by remember{ mutableStateOf(0x00170003U)}
                    var dutyUnused by remember{ mutableStateOf(dutycleValue)}
                    CamperFanControlView(
                        temperatureValue = temperatureValue,
                        thresholdValue = thresholdValue,
                        dutyCycleValue = dutycleValue,
                        fan2Value = fan2Value,
                        powerOnState=powerOnState,
                        onPowerOnChanged = { viewModel.powerOn(it) },
                        ledsState = ledsState,
                        ledsChanged =  { viewModel.ledsOn(it) },
//                        dutyCycleChanged = {viewModel.setDutyCycle(it)},
//                        fan2Changed = {viewModel.setFan2(it)},
//                        thresholdChanged = {viewModel.setThreshold(it)},
                        settingsViewModel = dutyViewModel,
                        dutyUnused = dutyUnused,
                        dutySetCallback = {force, value -> viewModel.setDutyCycle(force, value)},
                        thresholdCallback =  {force, value -> viewModel.setThreshold(force, value)},
                        fanCallback = {force, value -> viewModel.setFan2(force, value)},
                        modifier = Modifier
                            .widthIn(max = 460.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(2.dp),
                        pswViewModel = pswViewModel,
                        pswCallback = {force, value -> viewModel.updatePassword(value)},
                    )

                     */
                    NavigationBarExample(modifier = Modifier)
                }
                CamperFan.State.NOT_AVAILABLE -> {
                    DeviceDisconnectedView(
                        reason = Reason.LINK_LOSS,
                        modifier = Modifier.padding(16.dp),
                    ) { padding ->
                        Button(
                            onClick = { viewModel.connect() },
                            modifier = Modifier.padding(padding),
                        ) {
                            Text(text = stringResource(id = R.string.action_retry))
                        }
                    }
                }
            }
        }
    }
}
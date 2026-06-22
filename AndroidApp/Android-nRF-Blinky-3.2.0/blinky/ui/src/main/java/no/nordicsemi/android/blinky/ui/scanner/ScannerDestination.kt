package no.nordicsemi.android.blinky.ui.scanner

import androidx.hilt.navigation.compose.hiltViewModel
import no.nordicsemi.android.blinky.ui.control.CamperFan
import no.nordicsemi.android.blinky.ui.control.FanDevice
import no.nordicsemi.android.blinky.ui.scanner.view.CamperFanScanner
import no.nordicsemi.android.common.navigation.createSimpleDestination
import no.nordicsemi.android.common.navigation.defineDestination
import no.nordicsemi.android.common.navigation.viewmodel.SimpleNavigationViewModel

val Scanner = createSimpleDestination("scanner")

val ScannerDestination = defineDestination(Scanner) {
    val viewModel: SimpleNavigationViewModel = hiltViewModel()

    CamperFanScanner(
        onDeviceSelected = { device, name ->
            viewModel.navigateTo(CamperFan, FanDevice(device, name))
        }
    )
}
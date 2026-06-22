package no.nordicsemi.android.blinky.ui.control

import android.bluetooth.BluetoothDevice
import android.os.Parcelable
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.parcelize.Parcelize
import no.nordicsemi.android.blinky.ui.control.view.CamperFanScreen
import no.nordicsemi.android.common.navigation.createDestination
import no.nordicsemi.android.common.navigation.defineDestination
import no.nordicsemi.android.common.navigation.viewmodel.SimpleNavigationViewModel

val CamperFan = createDestination<FanDevice, Unit>("CamperFan")

@Parcelize
data class FanDevice(
    val device: BluetoothDevice,
    val name: String?,
): Parcelable

val FanDestination = defineDestination(CamperFan) {
    val viewModel: SimpleNavigationViewModel = hiltViewModel()

    CamperFanScreen (
        onNavigateUp = { viewModel.navigateUp() }
    )
}

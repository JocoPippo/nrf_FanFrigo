package no.nordicsemi.android.blinky.ble.data

import android.bluetooth.BluetoothDevice
import no.nordicsemi.android.ble.data.Data

class PowerOnData : PowerOnCallback() {
    var state: Boolean = false

    override fun onPowerOnStateChanged(device: BluetoothDevice, state: Boolean) {
        this.state = state
    }
    companion object {
        fun from(value: Boolean): Data {
            return Data.opCode(if (value) 0x01 else 0x00)
        }
    }

}
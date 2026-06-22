package no.nordicsemi.android.blinky.ble.data

import android.bluetooth.BluetoothDevice

class TemperatureData : TemperatureCallback() {
    var state: UInt = 0x00120000U

    override fun onTemperatureValueChanged(device: BluetoothDevice, state: UInt) {
        this.state = state
    }
}
package no.nordicsemi.android.blinky.ble.data

import android.bluetooth.BluetoothDevice
import no.nordicsemi.android.ble.data.Data

class Fan2Data : Fan2Callback() {
    var state: UByte = 0x00.toUByte()

    override fun onFan2StateChanged(device: BluetoothDevice, state: UByte) {
        this.state = state
    }
    companion object {
        fun from(value: UByte): Data {
            return Data.opCode(value.toByte())
        }
    }

}
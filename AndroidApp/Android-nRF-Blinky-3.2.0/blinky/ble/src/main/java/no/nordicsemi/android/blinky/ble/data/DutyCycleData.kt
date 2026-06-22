package no.nordicsemi.android.blinky.ble.data

import android.bluetooth.BluetoothDevice
import no.nordicsemi.android.ble.data.Data

class DutyCycleData : DutyCycleCallback() {
    var state: UInt = 0U

    override fun onDutyCycleValueChanged(device: BluetoothDevice, state: UInt) {
        this.state = state
    }

    companion object {
        fun get(value: UInt): Data {
            val data = ByteArray(4)

            data[0] = (value shr 24).toByte()
            data[1] = (value shr 16).toByte()
            data[2] = (value shr 8).toByte()
            data[3] = (value and 0xFFU).toByte()

            return Data(data)

        }
    }

}
package no.nordicsemi.android.blinky.ble.data

import android.bluetooth.BluetoothDevice
import no.nordicsemi.android.ble.data.Data

class TempThresholdData : TempThresholdCallback() {
    var state: UInt = 0x00280000U

    override fun onTempThresholdValueChanged(device: BluetoothDevice, state: UInt) {
        this.state = state
    }

    companion object {
        //LPP TO MODIFY accordingly with required format
        fun get(value: UInt): Data {
            val data: ByteArray = ByteArray(4)

            data[0] = (value shr 24).toByte()
            data[1] = (value shr 16).toByte()
            data[2] = (value shr 8).toByte()
            data[3] = (value).toByte()

            return Data(data)

        }
    }
}
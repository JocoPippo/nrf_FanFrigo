package no.nordicsemi.android.blinky.ble.data

import android.bluetooth.BluetoothDevice
import no.nordicsemi.android.ble.callback.profile.ProfileReadResponse
import no.nordicsemi.android.ble.data.Data

abstract class Fan2Callback : ProfileReadResponse() {

    override fun onDataReceived(device: BluetoothDevice, data: Data) {
        if (data.size() == 1) {
            val fan2State = data.getByte(0)?.toUByte() ?: 0x00.toUByte()
            onFan2StateChanged(device, fan2State)

        } else {
            onInvalidDataReceived(device, data)
        }
    }

    abstract fun onFan2StateChanged(device: BluetoothDevice, state: UByte)
}
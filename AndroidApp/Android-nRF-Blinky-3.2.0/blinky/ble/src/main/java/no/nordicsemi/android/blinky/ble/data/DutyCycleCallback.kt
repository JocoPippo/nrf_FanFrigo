package no.nordicsemi.android.blinky.ble.data

import android.bluetooth.BluetoothDevice
import no.nordicsemi.android.ble.callback.profile.ProfileReadResponse
import no.nordicsemi.android.ble.data.Data

abstract class DutyCycleCallback : ProfileReadResponse() {

    override fun onDataReceived(device: BluetoothDevice, data: Data) {
        if (data.size() == 4) {
            val dutyValue = data.getIntValue(Data.FORMAT_UINT32_LE, 0)
            if (dutyValue != null) {
                onDutyCycleValueChanged(device, dutyValue.toUInt())
            }
        } else {
            onInvalidDataReceived(device, data)
        }
    }

    abstract fun onDutyCycleValueChanged(device: BluetoothDevice, state: UInt)
}
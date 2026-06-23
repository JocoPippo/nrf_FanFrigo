package no.nordicsemi.android.blinky.ble.data

import android.bluetooth.BluetoothDevice
import no.nordicsemi.android.ble.callback.profile.ProfileReadResponse
import no.nordicsemi.android.ble.data.Data


abstract class TemperatureCallback : ProfileReadResponse() {

    override fun onDataReceived(device: BluetoothDevice, data: Data) {
        if (data.size() == 4) {
            val temperatureValue = data.getIntValue(Data.FORMAT_UINT32_LE, 0)
            if (temperatureValue != null) {
                onTemperatureValueChanged(device, (temperatureValue.toUInt()))
            }
        } else {
            onInvalidDataReceived(device, data)
        }
    }

    abstract fun onTemperatureValueChanged(device: BluetoothDevice, state: UInt)
}
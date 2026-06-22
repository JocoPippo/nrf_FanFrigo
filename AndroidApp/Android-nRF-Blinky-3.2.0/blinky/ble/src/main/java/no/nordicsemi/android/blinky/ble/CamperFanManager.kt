package no.nordicsemi.android.blinky.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.ktx.asValidResponseFlow
import no.nordicsemi.android.ble.ktx.getCharacteristic
import no.nordicsemi.android.ble.ktx.state.ConnectionState
import no.nordicsemi.android.ble.ktx.stateAsFlow
import no.nordicsemi.android.ble.ktx.suspend
import no.nordicsemi.android.blinky.ble.data.DutyCycleCallback
import no.nordicsemi.android.blinky.ble.data.DutyCycleData
import no.nordicsemi.android.blinky.ble.data.Fan2Callback
import no.nordicsemi.android.blinky.ble.data.Fan2Data
import no.nordicsemi.android.blinky.ble.data.LedCallback
import no.nordicsemi.android.blinky.ble.data.LedData
import no.nordicsemi.android.blinky.ble.data.PowerOnCallback
import no.nordicsemi.android.blinky.ble.data.PowerOnData
import no.nordicsemi.android.blinky.ble.data.TempThresholdCallback
import no.nordicsemi.android.blinky.ble.data.TempThresholdData
import no.nordicsemi.android.blinky.ble.data.TemperatureCallback
import no.nordicsemi.android.blinky.ble.data.TemperatureData
import no.nordicsemi.android.blinky.spec.CamperFan
import no.nordicsemi.android.blinky.spec.CamperFanSpec
import timber.log.Timber

class CamperFanManager(
    context: Context,
    device: BluetoothDevice
): CamperFan by CamperFanManagerImpl(context, device)

private class CamperFanManagerImpl(
    context: Context,
    private val device: BluetoothDevice,
): BleManager(context), CamperFan {
    private val scope = CoroutineScope(Dispatchers.IO)

    private var temperatureCharacteristic: BluetoothGattCharacteristic? = null
    private var dutyCycleCharacteristic: BluetoothGattCharacteristic? = null
    private var enableFan2Characteristic: BluetoothGattCharacteristic? = null
    private var tempThresholdCharacteristic: BluetoothGattCharacteristic? = null
    private var powerOnCharacteristic: BluetoothGattCharacteristic? = null
    private var ledsCharacteristic: BluetoothGattCharacteristic? = null
    private var pswCharacteristic: BluetoothGattCharacteristic? = null

    private val _tempValue = MutableStateFlow(0x00120000U)
    override val tempValue = _tempValue.asStateFlow()

    private val _dutyCycleValue = MutableStateFlow(0U)
    override val dutyCycleValue = _dutyCycleValue.asStateFlow()

    private val _fan2State = MutableStateFlow(0x00.toUByte())
    override val fan2State = _fan2State.asStateFlow()

    private val _tempThresholdValue = MutableStateFlow(0x00280000U)
    override val tempThresholdValue = _tempThresholdValue.asStateFlow()

    private val _powerOnState = MutableStateFlow(false)
    override val powerOnState = _powerOnState.asStateFlow()

    private val _ledsOnState = MutableStateFlow(false)
    override val ledsOnState = _ledsOnState.asStateFlow()

    override val state = stateAsFlow()
        .map {
            when (it) {
                is ConnectionState.Connecting,
                is ConnectionState.Initializing -> CamperFan.State.LOADING
                is ConnectionState.Ready -> CamperFan.State.READY
                is ConnectionState.Disconnecting,
                is ConnectionState.Disconnected -> CamperFan.State.NOT_AVAILABLE
            }
        }
        .stateIn(scope, SharingStarted.Lazily, CamperFan.State.NOT_AVAILABLE)


    private val temperatureCallback by lazy {
        object : TemperatureCallback() {
            override fun onTemperatureValueChanged(device: BluetoothDevice, state: UInt) {
                _tempValue.tryEmit(state)
            }
        }
    }


    private val dutyCycleCallback by lazy {
        object : DutyCycleCallback() {
            override fun onDutyCycleValueChanged(device: BluetoothDevice, state: UInt) {
                _dutyCycleValue.tryEmit(state)
            }
        }
    }

    private val tempThresholdCallback by lazy {
        object : TempThresholdCallback() {
            override fun onTempThresholdValueChanged(device: BluetoothDevice, state: UInt) {
                _tempThresholdValue.tryEmit(state)
            }
        }
    }

    private val powerOnCallback by lazy {
        object : PowerOnCallback() {
            override fun onPowerOnStateChanged(device: BluetoothDevice, state: Boolean) {
                _powerOnState.tryEmit(state)
            }
        }
    }

    private val fan2Callback by lazy {
        object : Fan2Callback() {
            override fun onFan2StateChanged(device: BluetoothDevice, state: UByte) {
                _fan2State.tryEmit(state)
            }
        }
    }

    private val ledCallback by lazy {
        object : LedCallback() {
            override fun onLedStateChanged(device: BluetoothDevice, state: Boolean) {
                _ledsOnState.tryEmit(state)
            }
        }
    }


    override suspend fun connect() = connect(device)
        .retry(3, 300)
        .useAutoConnect(false)
        .timeout(15000)
        .suspend()

    override fun release() {
        // Cancel all coroutines.
        scope.cancel()

        val wasConnected = isReady
        // If the device wasn't connected, it means that ConnectRequest was still pending.
        // Cancelling queue will initiate disconnecting automatically.
        cancelQueue()

        // If the device was connected, we have to disconnect manually.
        if (wasConnected) {
            disconnect().enqueue()
        }
    }

    override suspend fun setPowerState(value: Boolean) {
        // Write the value to the characteristic.
        writeCharacteristic(
            powerOnCharacteristic,
            PowerOnData.from(value),
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        ).suspend()

        // Update the state flow with the new value.
        _powerOnState.value = value
    }

    override suspend fun setTempThreshold(value: UInt) {
        // Write the value to the characteristic.
        writeCharacteristic(
            tempThresholdCharacteristic,
            TempThresholdData.get(value),
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        ).suspend()

        // Update the state flow with the new value.
        //_tempThresholdValue.value = ( value and 0x7FFFFFFFU)
        _tempThresholdValue.value = value
        readCharacteristic(tempThresholdCharacteristic)
            .with(tempThresholdCallback)
            .enqueue()
    }

    override suspend fun setFan2State(value: UByte) {
        // Write the value to the characteristic.
        writeCharacteristic(
            enableFan2Characteristic,
            Fan2Data.from(value),
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        ).suspend()

        // Update the state flow with the new value.
        _fan2State.value = value
        readCharacteristic(enableFan2Characteristic)
            .with(fan2Callback)
            .enqueue()
    }

    override suspend fun setDutyCycleValue(value: UInt) {
        // Write the value to the characteristic.
        writeCharacteristic(
            dutyCycleCharacteristic,
            DutyCycleData.get(value),
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        ).suspend()

        // Update the state flow with the new value.
        _dutyCycleValue.value = (value and 0x7FFFFFFFU)
    }

    override suspend fun setLedsState(value: Boolean) {
        // Write the value to the characteristic.
        writeCharacteristic(
            ledsCharacteristic,
            LedData.from(value),
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        ).suspend()

        // Update the state flow with the new value.
        _ledsOnState.value = value
    }
    override suspend fun setPassword(value: UInt) {
 //LPP TO BE IMPLEMENT the pswCharacteristic
        // Write the value to the characteristic.
        writeCharacteristic(
            pswCharacteristic,
            DutyCycleData.get(value),
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        ).suspend()

    }

    override fun log(priority: Int, message: String) {
        Timber.log(priority, message)
    }

    override fun getMinLogPriority(): Int {
        // By default, the library logs only INFO or
        // higher priority messages. You may change it here.
        return Log.VERBOSE
    }

    override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
        // Get the LBS Service from the gatt object.
        gatt.getService(CamperFanSpec.BLINKY_SERVICE_UUID)?.apply {
//            // Get the LED characteristic.
//            ledCharacteristic = getCharacteristic(
//                BlinkySpec.BLINKY_LED_CHARACTERISTIC_UUID,
//                // Mind, that below we pass required properties.
//                // If your implementation supports only WRITE_NO_RESPONSE,
//                // change the property to BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE.
//                BluetoothGattCharacteristic.PROPERTY_WRITE
//            )
//            // Get the Button characteristic.
//            buttonCharacteristic = getCharacteristic(
//                BlinkySpec.BLINKY_BUTTON_CHARACTERISTIC_UUID,
//                BluetoothGattCharacteristic.PROPERTY_NOTIFY
//            )

            temperatureCharacteristic = getCharacteristic(
                CamperFanSpec.BLINKY_TEMPERATURE_CHARACTERISTIC_UUID,
                // Mind, that below we pass required properties.
                // If your implementation supports only WRITE_NO_RESPONSE,
                // change the property to BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE.
                BluetoothGattCharacteristic.PROPERTY_NOTIFY
            )
            // Get the  characteristic.
            tempThresholdCharacteristic = getCharacteristic(
                CamperFanSpec.BLINKY_TEMP_THRESHOLD_CHARACTERISTIC_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE
            )

            dutyCycleCharacteristic = getCharacteristic(
                CamperFanSpec.BLINKY_DUTYCYCLE_CHARACTERISTIC_UUID,
                // Mind, that below we pass required properties.
                // If your implementation supports only WRITE_NO_RESPONSE,
                // change the property to BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE.
                BluetoothGattCharacteristic.PROPERTY_NOTIFY
            )
            // Get the  characteristic.
            enableFan2Characteristic = getCharacteristic(
                CamperFanSpec.BLINKY_ENABLEFAN2_CHARACTERISTIC_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE
            )

            powerOnCharacteristic = getCharacteristic(
                CamperFanSpec.BLINKY_POWERON_CHARACTERISTIC_UUID,
                // Mind, that below we pass required properties.
                // If your implementation supports only WRITE_NO_RESPONSE,
                // change the property to BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE.
                BluetoothGattCharacteristic.PROPERTY_WRITE
            )
            ledsCharacteristic = getCharacteristic(
                CamperFanSpec.BLINKY_LEDS_CHARACTERISTIC_UUID,
                // Mind, that below we pass required properties.
                // If your implementation supports only WRITE_NO_RESPONSE,
                // change the property to BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE.
                BluetoothGattCharacteristic.PROPERTY_WRITE
            )
            pswCharacteristic = getCharacteristic(
                CamperFanSpec.BLINKY_PSW_CHARACTERISTIC_UUID,
                // Mind, that below we pass required properties.
                // If your implementation supports only WRITE_NO_RESPONSE,
                // change the property to BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE.
                BluetoothGattCharacteristic.PROPERTY_WRITE
            )

            // Return true if all required characteristics are supported.
            return powerOnCharacteristic != null && enableFan2Characteristic != null && dutyCycleCharacteristic != null &&
                   tempThresholdCharacteristic != null && temperatureCharacteristic != null && ledsCharacteristic != null
                    && pswCharacteristic != null
        }
        return false
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun initialize() {
        // Enable notifications for the button characteristic.
//        val flow: Flow<ButtonState> = setNotificationCallback(buttonCharacteristic)
//            .asValidResponseFlow()
        // Enable notifications for the Temperature characteristic.
        val temp: Flow<TemperatureData> = setNotificationCallback(temperatureCharacteristic)
            .asValidResponseFlow()
        // Enable notifications for the DutyCycle characteristic.
        val duty: Flow<DutyCycleData> = setNotificationCallback(dutyCycleCharacteristic)
            .asValidResponseFlow()
        // Forward the button state to the buttonState flow.
//        scope.launch {
//            flow.map { it.state }.collect { _buttonState.tryEmit(it) }
//        }
        scope.launch {
            temp.map { it.state }.collect { _tempValue.tryEmit(it) }
        }
        scope.launch {
            duty.map { it.state }.collect { _dutyCycleValue.tryEmit(it) }
        }

//        enableNotifications(buttonCharacteristic)
//            .enqueue()

        enableNotifications(temperatureCharacteristic)
            .enqueue()
        enableNotifications(dutyCycleCharacteristic)
            .enqueue()

        // Read the initial value of the button characteristic.
//        readCharacteristic(buttonCharacteristic)
//            .with(buttonCallback)
//            .enqueue()

        // Read the initial value of the LED characteristic.
//        readCharacteristic(ledCharacteristic)
//            .with(ledCallback)
//            .enqueue()


        // Read the initial value of the Temperature characteristic.
        readCharacteristic(temperatureCharacteristic)
            .with(temperatureCallback)
            .enqueue()

        // Read the initial value of the Temperature Threshold characteristic.
        readCharacteristic(tempThresholdCharacteristic)
            .with(tempThresholdCallback)
            .enqueue()

        // Read the initial value of the Dut Cycle characteristic.
        readCharacteristic(dutyCycleCharacteristic)
            .with(dutyCycleCallback)
            .enqueue()

        // Read the initial value of the Enable 2 Fan characteristic.
        readCharacteristic(enableFan2Characteristic)
            .with(fan2Callback)
            .enqueue()

        // Read the initial value of the PowerOn characteristic.
        readCharacteristic(powerOnCharacteristic)
            .with(powerOnCallback)
            .enqueue()
        // Read the initial value of the LedsOn characteristic.
        readCharacteristic(ledsCharacteristic)
            .with(ledCallback)
            .enqueue()

    }

    override fun onServicesInvalidated() {
//        ledCharacteristic = null
//        buttonCharacteristic = null
        temperatureCharacteristic = null
        tempThresholdCharacteristic = null
        dutyCycleCharacteristic = null
        enableFan2Characteristic = null
        powerOnCharacteristic = null
        ledsCharacteristic = null
        pswCharacteristic = null
    }
}
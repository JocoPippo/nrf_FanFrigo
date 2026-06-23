package no.nordicsemi.android.blinky.ui.control.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import no.nordicsemi.android.blinky.ble.data.float2Int
import no.nordicsemi.android.blinky.ble.data.setForced4Int
import no.nordicsemi.android.blinky.ble.setForced
import no.nordicsemi.android.blinky.ui.control.repository.CamperFanRepository
import no.nordicsemi.android.common.logger.LoggerLauncher
import javax.inject.Inject
import javax.inject.Named

/**
 * The view model for the CamperFan screen.
 *
 * @param context The application context.
 * @property repository The repository that will be used to interact with the device.
 * @property deviceName The name of the Blinky device, as advertised.
 */
@HiltViewModel
class CamperFanViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val repository: CamperFanRepository,
    @Named("deviceName") val deviceName: String,
) : AndroidViewModel(context as Application) {
    /** The connection state of the device. */
    val state = repository.state
    /** The LED state. */
    //val ledState = repository.loggedLedState
    //    .stateIn(viewModelScope, SharingStarted.Lazily, false)
    /** The button state. */
//    val buttonState = repository.loggedButtonState
//        .onEach { state ->
//            // Play a sound when the button is pressed.
//            try {
//                if (state) {
//                    val notification =
//                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
//                    val r = RingtoneManager.getRingtone(context, notification)
//                    r.play()
//                }
//            } catch (e: Exception) {
//                Timber.e("Failed to play notification sound")
//            }
//        }
//        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val temperatureValue = repository.loggedTemperatureValue
        .stateIn(viewModelScope, SharingStarted.Lazily, 0x001a0000U)

    val thresholdValue = repository.loggedThresholdValue
        .stateIn(viewModelScope, SharingStarted.Lazily, 0x002d0000U)


    val dutyCycleValue = repository.loggedDutyCycleValue
        .stateIn(viewModelScope, SharingStarted.Lazily, 0x000a0000U)

    val fan2Value = repository.loggedFan2State
        .stateIn(viewModelScope, SharingStarted.Lazily, 0x00.toUByte())

   val powerOnState = repository.loggedPowerOnState
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    val ledsOnState = repository.loggedLEDsState
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    init {
        // In this sample we want to connect to the device as soon as the view model is created.
        connect()
    }

    /**
     * Connects to the device.
     */
    fun connect() {
        val exceptionHandler = CoroutineExceptionHandler { _, _ -> }
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            // This method may throw an exception if the connection fails,
            // Bluetooth is disabled, etc.
            // The exception will be caught by the exception handler and will be ignored.
            repository.connect()
        }
    }

//    /**
//     * Sends a command to the device to toggle the LED state.
//     * @param on The new state of the LED.
//     */
//    fun turnLed(on: Boolean) {
//        val exceptionHandler = CoroutineExceptionHandler { _, _ -> }
//        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
//            // Just like above, when this method throws an exception, it will be caught by the
//            // exception handler and ignored.
//            repository.turnLed(on)
//        }
//    }
    /**
     * Sends a command to the device to toggle the PowerOn state.
     * @param on The new state of the PowerOn.
     */
    fun powerOn(on: Boolean) {
        val exceptionHandler = CoroutineExceptionHandler { _, _ -> }
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            // Just like above, when this method throws an exception, it will be caught by the
            // exception handler and ignored.
            repository.setPowerState(on)
        }
    }
    /**
     * Sends a command to the device to toggle the LED state.
     * @param value The new duty cycle of the PWM.
     */
    fun setDutyCycle(value: Float) {
        val exceptionHandler = CoroutineExceptionHandler { _, _ -> }
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            // Just like above, when this method throws an exception, it will be caught by the
            // exception handler and ignored.
            repository.setDutyCycleValue(setForced4Int(data = float2Int(value ), isForced= true))
        }
    }

    fun setDutyCycle(forced: Boolean, value:String) {
        val data = value.toFloat()
        val exceptionHandler = CoroutineExceptionHandler { _, _ -> }
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            // Just like above, when this method throws an exception, it will be caught by the
            // exception handler and ignored.
            repository.setDutyCycleValue(setForced4Int(data = float2Int(data ), isForced= forced))
        }
    }

    /**
     * Sends a command to the device to toggle the FAN2 state.
     * @param on The new state of the FAN2.
     */
    //fun setFan2(on: Boolean) {
    fun setFan2(on: UByte) {
        val exceptionHandler = CoroutineExceptionHandler { _, _ -> }
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            // Just like above, when this method throws an exception, it will be caught by the
            // exception handler and ignored.
            repository.setFan2State(on)
        }
    }

    fun setFan2(forced:Boolean, on: UByte) {
        setFan2(setForced(forced, on))
    }

    fun setFan2(forced:Boolean, on: Boolean) {
        setFan2(setForced(forced, on))
    }

    /**
     * Sends a command to the device to toggle the FAN2 state.
     * @param value The new state of the FAN2.
     */
    fun setThreshold(value: Float) {
    //fun setThreshold(value: UInt) {
        val exceptionHandler = CoroutineExceptionHandler { _, _ -> }
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            // Just like above, when this method throws an exception, it will be caught by the
            // exception handler and ignored.
            repository.setTempThreshold(setForced4Int(data = float2Int(value ), isForced= true))
            //repository.setTempThreshold(value)
        }
    }

    fun setThreshold(forced:Boolean, value: String) {
        val data = value.toFloat()
        val exceptionHandler = CoroutineExceptionHandler { _, _ -> }
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            // Just like above, when this method throws an exception, it will be caught by the
            // exception handler and ignored.
            repository.setTempThreshold(setForced4Int(data = float2Int(data), isForced= forced))
            //repository.setTempThreshold(value)
        }
    }

    /**
     * Sends a command to the device to toggle the LEDs state.
     * @param on The new state of the LEDs.
     */
    fun ledsOn(on: Boolean) {
        val exceptionHandler = CoroutineExceptionHandler { _, _ -> }
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            // Just like above, when this method throws an exception, it will be caught by the
            // exception handler and ignored.
            repository.setLedsState(on)
        }
    }


    fun updatePassword(newPassword: UInt) {
        val exceptionHandler = CoroutineExceptionHandler { _, _ -> }
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            // Just like above, when this method throws an exception, it will be caught by the
            // exception handler and ignored.
            repository.setPassword(newPassword)
        }
        //_password.value = newPassword
    }
    /**
     * Opens nRF Logger app with the log or Google Play if the app is not installed.
     */
    fun openLogger() {
        LoggerLauncher.launch(getApplication(), repository.logSession)
    }

    override fun onCleared() {
        super.onCleared()
        repository.release()
    }
}
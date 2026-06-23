package no.nordicsemi.android.blinky.spec

import kotlinx.coroutines.flow.StateFlow

interface CamperFan {

    enum class State {
        LOADING,
        READY,
        NOT_AVAILABLE
    }

    /**
     * Connects to the device.
     */
    suspend fun connect()

    /**
     * Disconnects from the device.
     */
    fun release()

    /**
     * The current state of the blinky.
     */
    val state: StateFlow<State>

    /**
     * valore temperatura
     */
    val tempValue: StateFlow<UInt>

    /**
     * valore PWM ventole
     */
    val dutyCycleValue: StateFlow<UInt>

    /**
     * stato seconda ventola
     */
    val fan2State: StateFlow<UByte>

    /**
     * valore soglia temperatura
     */
    val tempThresholdValue: StateFlow<UInt>

    /**
     * stato alimentazione
     */
    val powerOnState: StateFlow<Boolean>

    /**
     * stato LEDs
     */
    val ledsOnState: StateFlow<Boolean>

 //   val _password: StateFlow<String>

    suspend fun setDutyCycleValue(value: UInt)

    suspend fun setFan2State(value: UByte)

    suspend fun setTempThreshold(value: UInt)

    suspend fun setPowerState(value: Boolean)

    suspend fun setLedsState(value: Boolean)

    suspend fun setPassword(value: UInt)

}
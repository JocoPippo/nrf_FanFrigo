package no.nordicsemi.android.blinky.ui.control.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import no.nordicsemi.android.blinky.ble.purgeForced
import no.nordicsemi.android.blinky.spec.CamperFan
import no.nordicsemi.android.log.ILogSession
import no.nordicsemi.android.log.LogContract
import no.nordicsemi.android.log.timber.nRFLoggerTree
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

/**
 *
 * @param context The application context.
 * @param deviceId The device ID.
 * @param deviceName The name of the CamperFan device, as advertised.
 * @property camperFan The CamperFan implementation.
 */
class CamperFanRepository @Inject constructor(
    @ApplicationContext context: Context,
    @Named("deviceId") deviceId: String,
    @Named("deviceName") deviceName: String,
    private val camperFan: CamperFan,
): CamperFan by camperFan {
    /** Timber tree that logs to nRF Logger. */
    private val tree: Timber.Tree

    /** If the nRF Logger is installed, this will allow to open the session. */
    internal val logSession: ILogSession?

    init {
        // Plant a new Tree that logs to nRF Logger.
        tree = nRFLoggerTree(context, null, deviceId, deviceName)
            .also { Timber.plant(it) }
            .also { logSession = it.session }
    }

    @OptIn(ExperimentalStdlibApi::class)
    val loggedTemperatureValue: Flow<UInt>
        get() = camperFan.tempValue.onEach {
            //val p = it.toHexString()
            Timber.log(LogContract.Log.Level.APPLICATION, "Temperature: ${it.toHexString()}")
        }

    @OptIn(ExperimentalStdlibApi::class)
    val loggedThresholdValue: Flow<UInt>
        get() = camperFan.tempThresholdValue.onEach {
            Timber.log(LogContract.Log.Level.APPLICATION, "Threshold: ${it.toHexString()}")
        }

    @OptIn(ExperimentalStdlibApi::class)
    val loggedDutyCycleValue: Flow<UInt>
        get() = camperFan.dutyCycleValue.onEach {
            Timber.log(LogContract.Log.Level.APPLICATION, "Dutycycle: ${it.toHexString()}")
        }

    @OptIn(ExperimentalStdlibApi::class)
    val loggedFan2State: Flow<UByte>
        get() = camperFan.fan2State.onEach {
            // Although Timber log levels are the same as LogCat's, nRF Logger has its own.
            // All standard log levels are mapped to the corresponding nRF Logger's levels:
            // https://github.com/NordicSemiconductor/nRF-Logger-API/blob/f90d5834c46cc2057b6a9f39dcbb8f2f2dd45d56/log-timber/src/main/java/no/nordicsemi/android/log/timber/nRFLoggerTree.java#L104
            // However, in order to log in nRF Logger on APPLICATION level, we need to use
            // that level explicitly.
            when(purgeForced(it) != 0x00.toUByte()) {
                true -> Timber.log(LogContract.Log.Level.APPLICATION, "Fan2 turned ON (${it.toHexString()})")
                false -> Timber.log(LogContract.Log.Level.APPLICATION, "Fan2 turned OFF  (${it.toHexString()})")
            }
        }

        val loggedPowerOnState: Flow<Boolean>
        get() = camperFan.powerOnState.onEach {
            // The same applies here.
            when(it) {
                true -> Timber.log(LogContract.Log.Level.APPLICATION, "PowerOn TRUE")
                false -> Timber.log(LogContract.Log.Level.APPLICATION, "PowerOn FALSE")
            }
        }

    val loggedLEDsState: Flow<Boolean>
        get() = camperFan.ledsOnState.onEach {
            // The same applies here.
            when(it) {
                true -> Timber.log(LogContract.Log.Level.APPLICATION, "LEDs ENABLED")
                false -> Timber.log(LogContract.Log.Level.APPLICATION, "LEDs DISABLED")
            }
        }

    override fun release() {
        Timber.uproot(tree)
        camperFan.release()
    }
}
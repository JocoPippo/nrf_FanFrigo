package no.nordicsemi.android.blinky.di

import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import no.nordicsemi.android.blinky.ble.CamperFanManager
import no.nordicsemi.android.blinky.ui.control.CamperFan
import no.nordicsemi.android.blinky.spec.CamperFan
import no.nordicsemi.android.blinky.spec.R
import no.nordicsemi.android.common.navigation.get
import javax.inject.Named

@Suppress("unused")
@Module
@InstallIn(ViewModelComponent::class)
abstract class CamperFanModule {

    companion object {

        @Provides
        @ViewModelScoped
        fun provideBluetoothDevice(handle: SavedStateHandle): BluetoothDevice {
            return handle.get(CamperFan).device
        }

        @Provides
        @ViewModelScoped
        @Named("deviceName")
        fun provideDeviceName(
            @ApplicationContext context: Context,
            handle: SavedStateHandle,
        ): String {
            return handle.get(CamperFan).name ?: context.getString(R.string.unnamed_device)
        }

        @Provides
        @ViewModelScoped
        @Named("deviceId")
        fun provideDeviceId(
            bluetoothDevice: BluetoothDevice
        ): String = bluetoothDevice.address

        @Provides
        @ViewModelScoped
        fun provideCamperFanManager(
            @ApplicationContext context: Context,
            device: BluetoothDevice,
        ) = CamperFanManager(context, device)

    }

    @Binds
    abstract fun bindCamperFan(
        camperFanManager: CamperFanManager
    ): CamperFan

}
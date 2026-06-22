package no.nordicsemi.android.blinky.spec

import java.util.UUID

class CamperFanSpec {

    companion object {
        val BLINKY_SERVICE_UUID: UUID = UUID.fromString("9b046cc7-dec2-438f-bde5-b093f4b5511c")

        //val BLINKY_TEMPERATURE_CHARACTERISTIC_UUID: UUID = UUID.fromString("00001526-1212-efde-1523-785feabcd123")
        val BLINKY_TEMPERATURE_CHARACTERISTIC_UUID: UUID = UUID.fromString("9b046cc8-dec2-438f-bde5-b093f4b5511c")
        val BLINKY_DUTYCYCLE_CHARACTERISTIC_UUID: UUID = UUID.fromString("9b046cc9-dec2-438f-bde5-b093f4b5511c")
        val BLINKY_ENABLEFAN2_CHARACTERISTIC_UUID: UUID = UUID.fromString("9b046cca-dec2-438f-bde5-b093f4b5511c")
        val BLINKY_TEMP_THRESHOLD_CHARACTERISTIC_UUID: UUID = UUID.fromString("9b046ccb-dec2-438f-bde5-b093f4b5511c")
        val BLINKY_POWERON_CHARACTERISTIC_UUID: UUID = UUID.fromString("9b046ccc-dec2-438f-bde5-b093f4b5511c")
        val BLINKY_LEDS_CHARACTERISTIC_UUID: UUID = UUID.fromString("9b046ccd-dec2-438f-bde5-b093f4b5511c")
        val BLINKY_PSW_CHARACTERISTIC_UUID: UUID = UUID.fromString("9b046cce-dec2-438f-bde5-b093f4b5511c")


    }

}
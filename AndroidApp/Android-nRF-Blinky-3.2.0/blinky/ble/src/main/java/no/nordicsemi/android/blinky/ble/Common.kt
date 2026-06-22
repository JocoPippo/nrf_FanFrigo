package no.nordicsemi.android.blinky.ble

import java.nio.ByteBuffer
import java.nio.ByteOrder


fun isForced(data: UInt): Boolean {
    return (data and 0x80000000U) != 0U
}
fun isForced(data: UByte): Boolean {
    return (data.toInt() and 0x80) != 0
}

fun purgeForced(data: UByte): UByte {
    return (data and 0x7F.toUByte())
}

fun purgeForced(data: UInt): UInt {
    return (data and 0x7FFFFFFFU)
}



fun fromUint2ByteArray(value: UInt): ByteArray {
    val bufferSize = Int.SIZE_BYTES
    val buffer = ByteBuffer.allocate(bufferSize)
    buffer.order(ByteOrder.BIG_ENDIAN) // BIG_ENDIAN is default byte order, so it is not necessary.
    buffer.putInt(value.toInt())
    return buffer.array()
}


fun fromUInt2Float(value: UInt): Float {
    var result = ((value and 0x7FFF0000U) shr 16).toFloat()

    result += ((value and 0x0000FFFFU).toFloat() / 100.0f)

    return result
}

fun setForced(forced: Boolean, value: UByte): UByte {
    if(forced) {
        return (value or 0x80.toUByte())
    }
    //else
    return value
}


fun setForced(forced: Boolean, value: Boolean): UByte {
    var result :UByte = if (value) 0x01.toUByte() else 0x00.toUByte()

    if(forced) {
        //return (value or 0x80.toUByte())
        result = result or 0x80.toUByte()
    }
    //else
        //return value
    return result
}


fun setForced(forced: Boolean, value: UInt): UInt {
    if(forced) {
        return (value or 0x80000000U)
    }
    //else
    return value
}

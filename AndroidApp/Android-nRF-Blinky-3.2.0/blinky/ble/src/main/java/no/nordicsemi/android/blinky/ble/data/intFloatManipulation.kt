package no.nordicsemi.android.blinky.ble.data



    fun int2Float(data: UInt) :Float {
        var result: Float
        var tmp: UInt = (data and 0x7FFF0000U) shr 16
        result = tmp.toFloat()
        tmp = data and 0X0000FFFFU
        result += (tmp.toFloat()/100.0f)
        return result
    }

    fun float2Int(data: Float) :UInt {
        var tmp: Float
        var result: UInt = data.toUInt() shl 16
        tmp = (data - result.toFloat()) * 100f
        result += tmp.toUInt() and 0X0000FFFFU
        return result
    }

fun getForced4Int(data: UInt) :Boolean {
    return ((data and 0x80000000U) != 0U)
}

fun setForced4Int(data: UInt, isForced: Boolean) :UInt {
    return (data or if (isForced) 0x80000000U else 0U)
}
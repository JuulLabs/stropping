package com.juul.stropping.mockk

import com.juul.stropping.ReplacementHandle
import io.mockk.mockk

inline fun <reified T : Any> ReplacementHandle.overwriteWithMockK(
    named: String? = null,
    relaxed: Boolean = false,
    relaxUnitFun: Boolean = false,
    configuration: T.() -> Unit = {}
): T {
    val mock = mockk(relaxed = relaxed, relaxUnitFun = relaxUnitFun, block = configuration)
    this.overwrite(mock, named)
    return mock
}

inline fun <reified Key : Any, reified MapValue : Any, reified MockValue : MapValue> ReplacementHandle.addMockKIntoMap(
    key: Key,
    named: String? = null,
    relaxed: Boolean = false,
    relaxUnitFun: Boolean = false,
    configuration: MockValue.() -> Unit = {}
): MapValue {
    val mock = mockk(
        relaxed = relaxed,
        relaxUnitFun = relaxUnitFun,
        block = configuration
    )
    this.addIntoMap<Key, MapValue>(key, mock, named)
    return mock
}

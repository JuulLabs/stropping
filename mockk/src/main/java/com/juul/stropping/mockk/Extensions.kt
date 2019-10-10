package com.juul.stropping.mockk

import com.juul.stropping.ReplacementHandle
import io.mockk.mockk

inline fun <reified T : Any> ReplacementHandle.overwriteWithMockK(
    relaxed: Boolean = false,
    relaxUnitFun: Boolean = false,
    configuration: T.() -> Unit = {}
): T {
    val mock = mockk(relaxed = relaxed, relaxUnitFun = relaxUnitFun, block = configuration)
    this.overwrite(mock)
    return mock
}

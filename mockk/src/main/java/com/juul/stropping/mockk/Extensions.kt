package com.juul.stropping.mockk

import com.juul.stropping.ReplacementHandle
import io.mockk.mockk

inline fun <reified T : Any> ReplacementHandle<*>.addMockk(
    relaxed: Boolean = false,
    relaxUnitFun: Boolean = false,
    configuration: T.() -> Unit = {}
): T = mockk(relaxed = relaxed, relaxUnitFun = relaxUnitFun, block = configuration)
    .also(this::add)

package com.juul.stropping.kodein

import io.mockk.mockk
import io.mockk.spyk

/** Sugar for calling [ReplacementHandle.overwrite] with a new [mockk]. Returns the mock. */
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

/** Sugar for creating a [spyk] from the value currently [ReplacementHandle.provide]d, then calling [ReplacementHandle.overwrite]. */
inline fun <reified T : Any> ReplacementHandle.overwriteWithSpyK(
    named: String? = null,
    configuration: T.() -> Unit = {}
): T {
    val spy = spyk(provide<T>(named), block = configuration)
    this.overwrite(spy, named)
    return spy
}

/** Sugar for calling [ReplacementHandle.addIntoMap] with a new `mockk`. Returns the mock. */
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

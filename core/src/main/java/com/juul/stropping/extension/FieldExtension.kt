package com.juul.stropping.extension

import java.lang.reflect.Field

/**
 * Do something with a [Field], guaranteeing it is accessible during [withAccess]. After the lambda
 * completes, the value of [Field.isAccessible] is returned to whatever the original value was.
 */
internal inline fun <T> Field.access(
    crossinline withAccess: Field.() -> T
): T {
    val wasAccessible = isAccessible
    isAccessible = true
    val blockResult = withAccess.invoke(this)
    isAccessible = wasAccessible
    return blockResult
}

/** Calls [Field.set] while guaranteeing that it accessible during the call. */
fun <T> Field.forceSet(
    receiver: Any,
    value: T
) = access {
    set(receiver, value)
}

/** Calls [Field.get] while guaranteeing that it accessible during the call. */
fun <T> Field.forceGet(
    receiver: Any
): T = access {
    @Suppress("UNCHECKED_CAST")
    get(receiver) as T
}

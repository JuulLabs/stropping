package com.juul.stropping.extension

import java.lang.reflect.Field

inline fun <T> Field.access(
    withAccess: Field.() -> T
): T {
    val wasAccessible = isAccessible
    isAccessible = true
    val blockResult = withAccess.invoke(this)
    isAccessible = wasAccessible
    return blockResult
}

fun <T> Field.forceSet(
    receiver: Any,
    value: T
) = access {
    set(receiver, value)
}

fun <T> Field.forceGet(
    receiver: Any
): T = access {
    @Suppress("UNCHECKED_CAST")
    get(receiver) as T
}

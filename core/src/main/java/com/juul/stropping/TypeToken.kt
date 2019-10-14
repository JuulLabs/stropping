package com.juul.stropping

import org.kodein.di.TypeToken
import java.lang.reflect.Type
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaType

/**
 * Constructor for a private class. Allows creating type-safe implementations of [TypeToken].
 *
 * Alternatively, it would be possible to just manually implement the interface, but that's more
 * work than I feel like doing.
 */
private val typeTokenConstructor: KFunction<TypeToken<Any>> by lazy {
    val tokenClass = Class.forName("org.kodein.di.ParameterizedTypeToken").kotlin
    @Suppress("UNCHECKED_CAST")
    tokenClass.primaryConstructor as KFunction<TypeToken<Any>>
}

/** Create a [TypeToken] for the given [type]. */
internal fun createTypeToken(type: Type): TypeToken<Any> {
    return typeTokenConstructor.call(type)
}

/** Create a [TypeToken] for the given [type]. */
internal fun createTypeToken(type: KType): TypeToken<Any> {
    return typeTokenConstructor.call(type.javaType)
}

package com.juul.stropping.utility

import org.kodein.di.TypeToken
import java.lang.reflect.Type
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaType

private val typeTokenConstructor: KFunction<TypeToken<Any>> by lazy {
    val tokenClass = Class.forName("org.kodein.di.ParameterizedTypeToken").kotlin
    @Suppress("UNCHECKED_CAST")
    tokenClass.primaryConstructor as KFunction<TypeToken<Any>>
}

internal fun createTypeToken(type: Type): TypeToken<Any> {
    return typeTokenConstructor.call(type)
}
internal fun createTypeToken(type: KType): TypeToken<Any> {
    return typeTokenConstructor.call(type.javaType)
}

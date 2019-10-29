package com.juul.stropping

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.jvm.javaType
import kotlin.reflect.typeOf

/** Internal type used to implement [javaTypeOf]. Otherwise useless. */
interface TypeHandle<T>

/** Creates a [Type] from a reified function call. Similar to [typeOf] except JVM-type friendly. */
inline fun <reified T : Any> javaTypeOf(): Type {
    val handle = object : TypeHandle<T> {}
    val supertype = handle::class.supertypes.first().javaType as ParameterizedType
    return supertype.actualTypeArguments.single()
}

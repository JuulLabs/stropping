package com.juul.stropping

import org.kodein.di.TypeToken
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaType

/** Internal type used to implement [javaTypeOf]. Otherwise useless. */
interface TypeHandle<T>

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
fun createTypeToken(type: Type): TypeToken<Any> {
    return typeTokenConstructor.call(type)
}

/** Create a [TypeToken] for the given [type]. */
fun createTypeToken(type: KType): TypeToken<Any> {
    return typeTokenConstructor.call(type.javaType)
}

/** Creates a [ParameterizedType] from this [Class]. */
internal fun Class<*>.parameterize(
    vararg params: Type
): ParameterizedType {
    require(params.size == this.typeParameters.size)
    return object : ParameterizedType {
        override fun getRawType(): Type = this@parameterize
        override fun getOwnerType(): Type? = null
        override fun getActualTypeArguments(): Array<out Type> = params

        override fun equals(other: Any?): Boolean {
            @Suppress("NAME_SHADOWING")
            val other = other as? ParameterizedType ?: return false
            return rawType == other.rawType
                && actualTypeArguments.contentEquals(other.actualTypeArguments)
        }

        override fun hashCode(): Int {
            return rawType.hashCode().xor(actualTypeArguments.contentHashCode())
        }
    }
}

inline fun <reified T : Any> javaTypeOf(): Type {
    val handle = object : TypeHandle<T> {}
    val supertype = handle::class.supertypes.first().javaType as ParameterizedType
    return supertype.actualTypeArguments.first()
}

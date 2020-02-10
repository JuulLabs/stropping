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

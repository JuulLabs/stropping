package com.juul.stropping.kodein

import android.util.Log
import com.juul.stropping.Injectable
import com.juul.stropping.InjectableConstructor
import com.juul.stropping.QualifiedType
import com.juul.stropping.common.forceGet
import com.juul.stropping.common.hasAnnotation
import org.kodein.di.Kodein
import org.kodein.di.TypeToken
import org.kodein.di.bindings.NoArgKodeinBinding
import org.kodein.di.conf.ConfigurableKodein
import org.kodein.di.direct
import org.kodein.di.fullDispString
import org.kodein.di.generic.provider
import org.kodein.di.generic.singleton
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import javax.inject.Singleton

// TODO: Make this less of a god function
@Suppress("UNCHECKED_CAST")
private fun <T : Any> ConfigurableKodein.getInstanceOf(typeToken: TypeToken<T>, tag: Any?): T {
    val type = typeToken::class.java.getDeclaredField("trueType")
        .forceGet<Type>(typeToken)
    val typeName = type.fullDispString()
    Log.d("Stropping", "Attemping to get instance of $typeName (tag=$tag)")
    if (type is ParameterizedType) {
        val raw = type.rawType
        if (raw == dagger.Lazy::class.java || raw == javax.inject.Provider::class.java) {
            Log.d("Stropping", "Found dagger.Lazy or javax.inject.Provider -- deferring injection")
            val paramType = type.actualTypeArguments.first()
            return object : dagger.Lazy<Any>, javax.inject.Provider<Any> {
                override fun get(): Any {
                    Log.d("Stropping", "Lazy/Provider get() for $typeName (tag=$tag)")
                    return getInstanceOf(createTypeToken(paramType), tag)
                }
            } as T
        }
        // TODO: Handle other param types.
    }
    val instance = try {
        direct.Instance(typeToken, tag)
    } catch (e: Kodein.NotFoundException) {
        Log.d(
            "Stropping",
            "Failed to inject $typeName (tag=$tag). Attempting automatic bind."
        )
        if (type is ParameterizedType) {
            throw UnsupportedOperationException("Cannot automatically bind parameterized type.", e)
        }
        check(type is Class<*>)
        addConfig {
            val isSingleton = type.hasAnnotation<Singleton>()
            val constructor = checkNotNull(InjectableConstructor.fromClass(type))
            val binding = when (isSingleton) {
                true -> singleton { inject(constructor) }
                false -> provider { inject(constructor) }
            } as NoArgKodeinBinding<Any, T>
            Bind(typeToken, tag) with binding
        }
        direct.Instance(typeToken, tag)
    }
    Log.d("Stropping", "Got $instance")
    return instance
}

internal fun <T : Any> ConfigurableKodein.getInstanceOf(qualifiedType: QualifiedType): T {
    @Suppress("UNCHECKED_CAST")
    return getInstanceOf(
        createTypeToken(qualifiedType.type) as TypeToken<T>,
        qualifiedType.qualifierString()
    )
}

internal fun <T : Any?> ConfigurableKodein.inject(injectable: Injectable<T>): T {
    return injectable.inject { qualifiedType -> getInstanceOf(qualifiedType) }
}

package com.juul.stropping

import android.util.Log
import org.kodein.di.Kodein
import org.kodein.di.TypeToken
import org.kodein.di.conf.ConfigurableKodein
import org.kodein.di.direct
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import javax.inject.Inject
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.jvmName

// TODO: Make this less of a god function
private fun <T : Any> ConfigurableKodein.getInstanceOf(typeToken: TypeToken<T>, tag: Any?): T {
    val type = typeToken::class.java.getDeclaredField("trueType")
        .forceGet<Type>(typeToken)
    Log.d("Stropping", "Attemping to get instance of ${type.typeName} (tag=$tag)")
    if (type is ParameterizedType) {
        if (type.rawType == Class.forName("dagger.Lazy")
            || type.rawType == Class.forName("javax.inject.Provider")
        ) {
            Log.d("Stropping", "Found dagger.Lazy or javax.inject.Provider -- deferring injection")
            val paramType = type.actualTypeArguments.first()
            @Suppress("UNCHECKED_CAST")
            return object : dagger.Lazy<Any>, javax.inject.Provider<Any> {
                override fun get(): Any {
                    Log.d("Stropping", "Lazy/Provider get() for ${paramType.typeName} (tag=$tag)")
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
            "Failed to inject ${type.typeName} (tag=$tag). Attempting automatic bind."
        )
        val annotation = type as? AnnotatedElement
        addConfig {
            val builderName = "constructor of ${type.typeName}"
            bind(type, annotation, builderName) { constructWithInjectedParameters(type) }
        }
        direct.Instance(typeToken, tag)
    }
    Log.d("Stropping", "Got $instance")
    return instance
}

internal fun <T : Any> ConfigurableKodein.getInstanceOf(type: KType, tag: Any?): T {
    @Suppress("UNCHECKED_CAST")
    return getInstanceOf(createTypeToken(type) as TypeToken<T>, tag)
}

internal fun <T : Any> ConfigurableKodein.getInstanceOf(type: Type, tag: Any?): T {
    @Suppress("UNCHECKED_CAST")
    return getInstanceOf(createTypeToken(type) as TypeToken<T>, tag)
}

internal fun ConfigurableKodein.injectIntoFields(receiver: Any) {
    Log.d("Stropping", "Injecting into @Inject fields of ${receiver::class.jvmName}")
    for (field in receiver::class.java.fieldsWithAnnotation<Inject>()) {
        val instance = getInstanceOf<Any>(field.type, getTag(field))
        field.forceSet(receiver, instance)
    }
}

internal fun <T : Any> getInjectableConstructor(kClass: KClass<T>): KCallable<T> {
    val constructors = kClass.constructors
    return constructors.singleOrNull()
        ?: constructors.single { it.hasAnnotation<Inject>() }
}

/**
 * Calls the [callable] (usually a function or a constructor) with Kodein-injected parameters.
 * If a [receiver] is specified, it will be used.
 */
internal fun <R> ConfigurableKodein.callWithInjectedParameters(
    callable: KCallable<R>,
    receiver: Any? = null
): R {
    val receiverName = "${receiver?.javaClass?.simpleName}."
    val logName = "$receiverName${callable.name}"
    Log.d("Stropping", "Getting parameters for $logName.")
    val params = callable.valueParameters
        .map {
            getInstanceOf<Any>(it.type, getTag(it))
        }.toTypedArray()
    Log.d("Stropping", "Calling $logName.")
    return if (receiver != null) {
        callable.call(receiver, *params)
    } else {
        callable.call(*params)
    }
}

/** Special case of [callWithInjectedParameters] which constructs a class given its type. */
internal fun <T : Any> ConfigurableKodein.constructWithInjectedParameters(kClass: KClass<T>): T {
    return callWithInjectedParameters(getInjectableConstructor(kClass))
}

/** Special case of [callWithInjectedParameters] which constructs a class given its type. */
internal fun <T : Any> ConfigurableKodein.constructWithInjectedParameters(type: Type): T {
    @Suppress("UNCHECKED_CAST")
    return callWithInjectedParameters(getInjectableConstructor((type as Class<T>).kotlin))
}

/** Special case of [callWithInjectedParameters] which constructs a class given its type. */
internal fun ConfigurableKodein.constructWithInjectedParameters(type: KType): Any {
    return constructWithInjectedParameters(type.classifier as KClass<*>)
}

package com.juul.stropping.extension

import com.juul.stropping.hasAnnotation
import com.juul.stropping.utility.createTypeToken
import org.kodein.di.DKodein
import javax.inject.Inject
import javax.inject.Named
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.valueParameters

/**
 * Calls the [callable] (usually a function or a constructor) with Kodein-injected parameters.
 * If a [receiver] is specified, it will be used.
 */
internal fun <R> DKodein.injectCall(callable: KCallable<R>, receiver: Any? = null): R {
    val params = callable.valueParameters
        .map {
            val tag = it.findAnnotation<Named>()?.value
            Instance(createTypeToken(it.type), tag)
        }.toTypedArray()
    return if (receiver != null) {
        callable.call(receiver, *params)
    } else {
        callable.call(*params)
    }
}

/** Special case of [injectCall] which constructs a class given its type. */
internal fun <T : Any> DKodein.injectConstructor(kClass: KClass<T>): T {
    val constructors = kClass.constructors
    val constructor = constructors.singleOrNull()
        ?: constructors.single { it.hasAnnotation<Inject>() }
    return injectCall(constructor)
}

/** Special case of [injectCall] which constructs a class given its type. */
internal fun DKodein.injectConstructor(type: KType): Any =
    injectConstructor(type.classifier as KClass<*>)

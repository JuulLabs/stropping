package com.juul.stropping.extension

import com.juul.stropping.fieldsWithAnnotation
import com.juul.stropping.getTag
import com.juul.stropping.hasAnnotation
import com.juul.stropping.utility.createTypeToken
import com.juul.stropping.utility.getModule
import com.juul.stropping.utility.getModulesForComponentClass
import dagger.Binds
import dagger.Component
import dagger.Module
import dagger.Provides
import org.kodein.di.Kodein
import org.kodein.di.TypeToken
import org.kodein.di.bindings.NoArgSimpleBindingKodein
import org.kodein.di.conf.ConfigurableKodein
import org.kodein.di.direct
import org.kodein.di.generic.provider
import org.kodein.di.generic.singleton
import java.lang.reflect.Type
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.javaType

private fun Kodein.Builder.bind(
    type: Type,
    element: KAnnotatedElement,
    build: NoArgSimpleBindingKodein<*>.() -> Any
) {
    val bind = Bind(createTypeToken(type), getTag(element))
    if (element.hasAnnotation<Singleton>()) {
        bind with singleton { this.build() }
    } else {
        bind with provider { this.build() }
    }
}

private fun Kodein.Builder.bind(
    type: KType,
    element: KAnnotatedElement,
    build: NoArgSimpleBindingKodein<*>.() -> Any
) = bind(type.javaType, element, build)

private fun Kodein.Builder.importBindsFunction(configurable: ConfigurableKodein, function: KFunction<*>) {
    require(function.hasAnnotation<Binds>())
    bind(type = function.returnType, element = function) {
        val implementationType = function.valueParameters.single().type
        configurable.constructWithInjectedParameters(implementationType)
    }
}

private fun Kodein.Builder.importProvidesFunction(configurable: ConfigurableKodein, moduleClass: KClass<*>, function: KFunction<*>) {
    require(function.hasAnnotation<Provides>())
    bind(type = function.returnType, element = function) {
        @Suppress("UNCHECKED_CAST")
        configurable.callWithInjectedParameters(function as KFunction<Any>, receiver = getModule(moduleClass))
    }
}

/** Imports from either a [Component] or [Module], ignoring submodules & includes. */
private fun Kodein.Builder.importDaggerFunctions(configurable: ConfigurableKodein, kClass: KClass<*>) {
    for (function in kClass.declaredMemberFunctions) {
        when {
            function.hasAnnotation<Binds>() -> importBindsFunction(configurable, function)
            function.hasAnnotation<Provides>() -> importProvidesFunction(configurable, kClass, function)
        }
    }
}

internal fun Kodein.Builder.importDaggerComponent(configurable: ConfigurableKodein, componentClass: KClass<*>) {
    importDaggerFunctions(configurable, componentClass)
    val modules = getModulesForComponentClass(componentClass)
    for (module in modules) {
        importDaggerFunctions(configurable, module)
    }
}

internal fun ConfigurableKodein.injectIntoFields(receiver: Any) {
    for (field in receiver::class.java.fieldsWithAnnotation<Inject>()) {
        val instance = getInstanceOf<Any>(field.type, getTag(field))
        field.forceSet(receiver, instance)
    }
}

private fun <T : Any> ConfigurableKodein.getInstanceOf(typeToken: TypeToken<T>, tag: Any?): T {
    if (tag != null) {
        return direct.Instance(typeToken, tag)
    }
    return try {
        direct.Instance(typeToken)
    } catch (e: Kodein.NotFoundException) {
        val clazz = e.key.type::class.java.getDeclaredField("trueType")
            .forceGet<Class<*>>(e.key.type)
        addConfig {
            bind(clazz, clazz.kotlin) { constructWithInjectedParameters(clazz.kotlin) }
        }
        direct.Instance(typeToken)
    }
}

internal fun <T : Any> ConfigurableKodein.getInstanceOf(type: KType, tag: Any?): T {
    @Suppress("UNCHECKED_CAST")
    return getInstanceOf(createTypeToken(type) as TypeToken<T>, tag)
}

internal fun <T : Any> ConfigurableKodein.getInstanceOf(type: Type, tag: Any?): T {
    @Suppress("UNCHECKED_CAST")
    return getInstanceOf(createTypeToken(type) as TypeToken<T>, tag)
}

/**
 * Calls the [callable] (usually a function or a constructor) with Kodein-injected parameters.
 * If a [receiver] is specified, it will be used.
 */
internal fun <R> ConfigurableKodein.callWithInjectedParameters(
    callable: KCallable<R>,
    receiver: Any? = null
): R {
    val params = callable.valueParameters
        .map {
            getInstanceOf<Any>(it.type, getTag(it))
        }.toTypedArray()
    return if (receiver != null) {
        callable.call(receiver, *params)
    } else {
        callable.call(*params)
    }
}

/** Special case of [callWithInjectedParameters] which constructs a class given its type. */
internal fun <T : Any> ConfigurableKodein.constructWithInjectedParameters(kClass: KClass<T>): T {
    val constructors = kClass.constructors
    val constructor = constructors.singleOrNull()
        ?: constructors.single { it.hasAnnotation<Inject>() }
    return callWithInjectedParameters(constructor)
}

/** Special case of [callWithInjectedParameters] which constructs a class given its type. */
internal fun ConfigurableKodein.constructWithInjectedParameters(type: KType): Any =
    constructWithInjectedParameters(type.classifier as KClass<*>)


package com.juul.stropping.extension

import android.util.Log
import com.juul.stropping.fieldsWithAnnotation
import com.juul.stropping.getTag
import com.juul.stropping.hasAnnotation
import com.juul.stropping.utility.createTypeToken
import com.juul.stropping.utility.getInjectableConstructor
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
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.ParameterizedType
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
    element: AnnotatedElement?,
    buildName: String,
    build: NoArgSimpleBindingKodein<*>.() -> Any
) {
    val tag = getTag(element)
    val bind = Bind(createTypeToken(type), tag)
    if (element?.hasAnnotation<Singleton>() == true) {
        Log.d("Stropping", "Binding singleton for $type (tag=$tag) with $buildName")
        bind with singleton { this.build() }
    } else {
        Log.d("Stropping", "Binding provider for $type (tag=$tag) with $buildName")
        bind with provider { this.build() }
    }
}

private fun Kodein.Builder.bind(
    type: Type,
    element: KAnnotatedElement?,
    buildName: String,
    build: NoArgSimpleBindingKodein<*>.() -> Any
) {
    val tag = getTag(element)
    val bind = Bind(createTypeToken(type), tag)
    if (element?.hasAnnotation<Singleton>() == true) {
        Log.d("Stropping", "Binding singleton for $type (tag=$tag) with $buildName")
        bind with singleton { this.build() }
    } else {
        Log.d("Stropping", "Binding provider for $type (tag=$tag) with $buildName")
        bind with provider { this.build() }
    }
}

private fun Kodein.Builder.bind(
    type: KType,
    element: KAnnotatedElement?,
    builderName: String,
    build: NoArgSimpleBindingKodein<*>.() -> Any
) = bind(type.javaType, element, builderName, build)

private fun Kodein.Builder.importBindsFunction(
    configurable: ConfigurableKodein,
    moduleClass: KClass<*>,
    function: KFunction<*>
) {
    require(function.hasAnnotation<Binds>())
    val builderName = "${moduleClass.simpleName}.${function.name}"
    bind(type = function.returnType, element = function, builderName = builderName) {
        val implementationType = function.valueParameters.single().type
        configurable.constructWithInjectedParameters(implementationType)
    }
}

private fun Kodein.Builder.importProvidesFunction(
    configurable: ConfigurableKodein,
    moduleClass: KClass<*>,
    function: KFunction<*>
) {
    require(function.hasAnnotation<Provides>())
    val builderName = "${moduleClass.simpleName}.${function.name}"
    bind(type = function.returnType, element = function, builderName = builderName) {
        @Suppress("UNCHECKED_CAST")
        configurable.callWithInjectedParameters(
            function as KFunction<Any>,
            receiver = getModule(moduleClass)
        )
    }
}

/** Imports from either a [Component] or [Module], ignoring submodules & includes. */
private fun Kodein.Builder.importDaggerFunctions(
    configurable: ConfigurableKodein,
    kClass: KClass<*>
) {
    for (function in kClass.declaredMemberFunctions) {
        when {
            function.hasAnnotation<Binds>() ->
                importBindsFunction(configurable, kClass, function)
            function.hasAnnotation<Provides>() ->
                importProvidesFunction(configurable, kClass, function)
        }
    }
}

internal fun Kodein.Builder.importDaggerComponent(
    configurable: ConfigurableKodein,
    componentClass: KClass<*>
) {
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
    val type = typeToken::class.java.getDeclaredField("trueType")
        .forceGet<Type>(typeToken)
    Log.d("Stropping", "Attemping to get instance of ${type.typeName} (tag=$tag)")
    if (type is ParameterizedType && type.rawType == Class.forName("dagger.Lazy")) {
        Log.d("Stropping", "Found dagger.Lazy -- deferring injection")
        val paramType = type.actualTypeArguments.first()
        @Suppress("UNCHECKED_CAST")
        return dagger.Lazy<Any> {
            Log.d("Stropping", "Lazy.get() for ${paramType.typeName} (tag=$tag)")
            getInstanceOf(createTypeToken(paramType), tag)
        } as T
    }
    val instance = try {
        direct.Instance(typeToken, tag)
    } catch (e: Kodein.NotFoundException) {
        Log.d("Stropping", "Failed to inject ${type.typeName} (tag=$tag). Attempting automatic bind.")
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


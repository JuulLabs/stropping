package com.juul.stropping.extension

import com.juul.stropping.fieldsWithAnnotation
import com.juul.stropping.hasAnnotation
import com.juul.stropping.utility.createTypeToken
import com.juul.stropping.utility.getModule
import com.juul.stropping.utility.getModulesForComponentClass
import dagger.Binds
import dagger.Component
import dagger.Module
import dagger.Provides
import org.kodein.di.Kodein
import org.kodein.di.bindings.NoArgSimpleBindingKodein
import org.kodein.di.conf.ConfigurableKodein
import org.kodein.di.direct
import org.kodein.di.generic.provider
import org.kodein.di.generic.singleton
import java.lang.reflect.Type
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.javaType

private fun Kodein.Builder.bind(
    type: Type,
    element: KAnnotatedElement,
    build: NoArgSimpleBindingKodein<*>.() -> Any
) {
    val tag = element.findAnnotation<Named>()?.value
    val bind = Bind(createTypeToken(type), tag)
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

private fun Kodein.Builder.importBindsFunction(function: KFunction<*>) {
    require(function.hasAnnotation<Binds>())
    bind(type = function.returnType, element = function) {
        val implementationType = function.valueParameters.single().type
        injectConstructor(implementationType)
    }
}

private fun Kodein.Builder.importProvidesFunction(moduleClass: KClass<*>, function: KFunction<*>) {
    require(function.hasAnnotation<Provides>())
    bind(type = function.returnType, element = function) {
        @Suppress("UNCHECKED_CAST")
        injectCall(function as KFunction<Any>, receiver = getModule(moduleClass))
    }
}

/** Imports from either a [Component] or [Module], ignoring submodules & includes. */
private fun Kodein.Builder.importDaggerFunctions(kClass: KClass<*>) {
    for (function in kClass.declaredMemberFunctions) {
        when {
            function.hasAnnotation<Binds>() -> importBindsFunction(function)
            function.hasAnnotation<Provides>() -> importProvidesFunction(kClass, function)
        }
    }
}

internal fun Kodein.Builder.importDaggerComponent(componentClass: KClass<*>) {
    importDaggerFunctions(componentClass)
    val modules = getModulesForComponentClass(componentClass)
    for (module in modules) {
        importDaggerFunctions(module)
    }
}

internal fun ConfigurableKodein.inject(receiver: Any) {
    for (field in receiver::class.java.fieldsWithAnnotation<Inject>()) {
        fun injectField() = field.forceSet(receiver, direct.Instance(createTypeToken(field.type)))
        try {
            injectField()
        } catch (e: Kodein.NotFoundException) {
            // Assumes class is of kodein private type `ParameterizedTypeToken`
            val clazz = e.key.type::class.java.getDeclaredField("trueType")
                .forceGet<Class<*>>(e.key.type)
            addConfig {
                bind(clazz, clazz.kotlin) { injectConstructor(clazz.kotlin) }
            }
            injectField()
        }
    }
}

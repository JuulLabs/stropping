package com.juul.stropping.extension

import com.juul.stropping.fieldsWithAnnotation
import com.juul.stropping.utility.createTypeToken
import com.juul.stropping.utility.getModule
import com.juul.stropping.utility.getModulesForComponentClass
import dagger.Binds
import dagger.Component
import dagger.Module
import dagger.Provides
import org.kodein.di.Kodein
import org.kodein.di.bindings.NoArgSimpleBindingKodein
import org.kodein.di.direct
import org.kodein.di.generic.provider
import org.kodein.di.generic.singleton
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.valueParameters

@UseExperimental(ExperimentalStdlibApi::class)
private fun Kodein.Builder.bind(
    type: KType,
    element: KAnnotatedElement,
    build: NoArgSimpleBindingKodein<*>.() -> Any
) {
    val bind = Bind(createTypeToken(type))
    if (element.hasAnnotation<Singleton>()) {
        bind with singleton { this.build() }
    } else {
        bind with provider { this.build() }
    }
}

@UseExperimental(ExperimentalStdlibApi::class)
private fun Kodein.Builder.importBindsFunction(function: KFunction<*>) {
    require(function.hasAnnotation<Binds>())
    bind(type = function.returnType, element = function) {
        val implementationType = function.valueParameters.single().type
        injectConstructor(implementationType)
    }
}

@UseExperimental(ExperimentalStdlibApi::class)
private fun Kodein.Builder.importProvidesFunction(moduleClass: KClass<*>, function: KFunction<*>) {
    require(function.hasAnnotation<Provides>())
    bind(type = function.returnType, element = function) {
        @Suppress("UNCHECKED_CAST")
        injectCall(function as KFunction<Any>, receiver = getModule(moduleClass))
    }
}

/** Imports from either a [Component] or [Module], ignoring submodules & includes. */
@UseExperimental(ExperimentalStdlibApi::class)
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

internal fun Kodein.inject(receiver: Any) {
    receiver::class.java.fieldsWithAnnotation<Inject>()
        .forEach { field ->
            field.forceSet(receiver, direct.Instance(createTypeToken(field.type)))
        }
}

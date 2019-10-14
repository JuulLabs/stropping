package com.juul.stropping

import dagger.Binds
import dagger.Component
import dagger.Module
import dagger.Provides
import org.kodein.di.Kodein
import org.kodein.di.conf.ConfigurableKodein
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.valueParameters

/** Instances of Dagger modules, so we can call [Provides]-annotated functions. */
private val providerModules = mutableMapOf<KClass<*>, Any>()

/**
 * Entry point for importing a dagger component, specified by the [componentClass] into Kodein.
 *
 * Note that [configurable] must have [ConfigurableKodein.mutable] as `true`.
 */
internal fun Kodein.Builder.importDaggerComponent(
    configurable: ConfigurableKodein,
    componentClass: KClass<*>
) {
    require(configurable.mutable == true)
    importDaggerFunctions(configurable, componentClass)
    val modules = getModulesForComponentClass(componentClass)
    for (module in modules) {
        importDaggerFunctions(configurable, module)
    }
}

/** Imports from either a [Component] or [Module], ignoring submodules & includes. */
private fun Kodein.Builder.importDaggerFunctions(
    configurable: ConfigurableKodein,
    kClass: KClass<*>
) {
    @Suppress("UNCHECKED_CAST")
    val functions = kClass.declaredMemberFunctions.map { it as KFunction<Any> }
    for (function in functions) {
        val builderName = "${kClass.simpleName}.${function.name}"
        when {
            function.hasAnnotation<Binds>() -> {
                bind(function.returnType, function, builderName) {
                    val implementationType = function.valueParameters.single().type
                    configurable.constructWithInjectedParameters(implementationType)
                }
            }
            function.hasAnnotation<Provides>() -> {
                bind(function.returnType, function, builderName) {
                    val receiver = providerModules.getOrPut(kClass) { kClass.java.newInstance() }
                    configurable.callWithInjectedParameters(function, receiver)
                }
            }
        }
    }
}


/**
 * Returns a list of all classes in the [Component] annotation's [Component.modules].
 *
 * Throws [IllegalArgumentException] if [componentClass] represents a generic class or is not
 * annotated with [Component].
 */
private fun getModulesForComponentClass(
    componentClass: KClass<*>
): List<KClass<*>> {
    require(componentClass.typeParameters.isEmpty())
    val component = requireNotNull(componentClass.findAnnotation<Component>())

    fun expandModules(moduleClass: KClass<*>): List<KClass<*>> {
        val modules = mutableListOf<KClass<*>>()
        fun expandModules(moduleClass: KClass<*>) {
            modules += moduleClass
            val includes = requireNotNull(moduleClass.findAnnotation<Module>()).includes
            for (submodule in includes) {
                expandModules(submodule)
            }
        }
        expandModules(moduleClass)
        return modules
    }

    return component.modules.flatMap(::expandModules)
}

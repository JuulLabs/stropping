package com.juul.stropping.utility

import com.juul.stropping.hasAnnotation
import dagger.Component
import dagger.Module
import dagger.Provides
import javax.inject.Inject
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

private val modules = mutableMapOf<KClass<*>, Any>()

internal fun <T : Any> getInjectableConstructor(kClass: KClass<T>): KCallable<T> {
    val constructors = kClass.constructors
    return constructors.singleOrNull()
        ?: constructors.single { it.hasAnnotation<Inject>() }
}

/**
 * Uses reflection to create an instance of [kClass]. This allows us to call [Provides] functions.
 *
 * Uses [ProxyFactory] to create instances of interfaces or abstract classes, and constructs
 * normal classes regularly.
 */
internal fun getModule(kClass: KClass<*>): Any = modules.getOrPut(kClass) {
    if (kClass.isAbstract) {
        TODO()
    } else {
        kClass.java.newInstance()
    }
}

/**
 * Returns a list of all classes in the [Component] annotation's [Component.modules].
 *
 * Throws [IllegalArgumentException] if [componentClass] represents a generic class or is not
 * annotated with [Component].
 */
internal fun getModulesForComponentClass(componentClass: KClass<*>): List<KClass<*>> {
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

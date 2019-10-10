package com.juul.stropping.utility

import dagger.Component
import dagger.Module
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

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

package com.juul.stropping

import dagger.Component
import dagger.Module

/**
 * Entry point for reflection over a Dagger graph.
 *
 * @constructor Creates an instance of a [Graph] for a given [Component]-annotated class.
 */
class Graph(componentClass: Class<*>) {
    companion object {
        /** Creates an instance of a [Graph] for a given [Component]-annotated class [T]. */
        inline fun <reified T : Any> of() = Graph(T::class.java)
    }

    init {
        require(componentClass.hasAnnotation<Component>())
    }

    /** The class of this dagger component and, recursively, all subcomponents and modules. */
    val includedClasses: Set<Class<*>> by lazy {
        val classes = mutableSetOf<Class<*>>()
        fun addClassAndIncludes(clazz: Class<*>) {
            classes += clazz
            val componentAnnotation = clazz.findAnnotation<Component>()
            val moduleAnnotation = clazz.findAnnotation<Module>()
            val directlyIncludedClasses = arrayOf(
                componentAnnotation?.modules.orEmpty(),
                componentAnnotation?.dependencies.orEmpty(),
                moduleAnnotation?.includes.orEmpty(),
                moduleAnnotation?.subcomponents.orEmpty()
            ).flatten()
            for (includedClass in directlyIncludedClasses) {
                addClassAndIncludes(includedClass.java)
            }
        }
        addClassAndIncludes(componentClass)
        classes
    }

    /** The [Provisioner]s found in [includedClasses]. */
    val provisioners: List<MethodProvisioner> by lazy {
        includedClasses.asSequence()
            .flatMap { it.declaredMethods.asSequence() }
            .mapNotNull { Provisioner.fromMethod(it) }
            .toList()
    }
}

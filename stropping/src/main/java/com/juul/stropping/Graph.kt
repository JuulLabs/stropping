package com.juul.stropping

import dagger.Component
import dagger.Module
import dagger.android.ContributesAndroidInjector
import java.lang.reflect.Method

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

    private val declaredMethods: Sequence<Method> by lazy {
        includedClasses.asSequence()
            .flatMap { it.declaredMethods.asSequence() }
    }

    /** The [Provisioner]s found in [includedClasses]. */
    val provisioners: List<Provisioner> by lazy {
        declaredMethods
            .mapNotNull { Provisioner.fromMethod(it) }
            .toList()
    }

    val androidInjectable: List<Class<*>> by lazy {
        declaredMethods
            .filter { it.hasAnnotation<ContributesAndroidInjector>() }
            .map { it.returnType }
            .toList()
    }
}

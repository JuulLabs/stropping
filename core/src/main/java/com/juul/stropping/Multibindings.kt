package com.juul.stropping

import dagger.MapKey
import dagger.multibindings.IntoMap
import dagger.multibindings.IntoSet
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Type
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaType

internal sealed class Multibindings {
    object Single : Multibindings()
    object ToSet : Multibindings()
    class ToMap(
        val keyType: Type,
        val keyValue: Any
    ) : Multibindings()

    companion object {
        private fun mapFrom(annotations: List<Annotation>): ToMap {
            require(annotations.any { it is IntoMap })
            val keyAnnotation = annotations.first { annotation ->
                val nestedAnnotations = annotation.annotationClass.annotations
                nestedAnnotations.any { it is MapKey }
            }
            val parameter = keyAnnotation.annotationClass
                .primaryConstructor!!
                .parameters.first()
            val keyType = parameter.type.javaType
            val keyValue = keyAnnotation.annotationClass.java.let { annotationClass ->
                try {
                    annotationClass.getMethod(parameter.name!!)
                        .invoke(keyAnnotation)!!
                } catch (e: NoSuchMethodException) {
                    annotationClass.getField(parameter.name!!)
                        .get(keyAnnotation)!!
                }
            }
            return ToMap(keyType, keyValue)
        }

        fun fromAnnotations(annotations: List<Annotation>): Multibindings = when {
            annotations.any { it is IntoMap } -> mapFrom(annotations)
            annotations.any { it is IntoSet } -> ToSet
            else -> Single
        }

        fun fromAnnotations(element: KAnnotatedElement): Multibindings =
            fromAnnotations(element.annotations)

        fun fromAnnotations(element: AnnotatedElement): Multibindings =
            fromAnnotations(element.annotations.toList())
    }
}

package com.juul.stropping

import com.juul.stropping.common.hasAnnotation
import java.lang.reflect.AnnotatedElement
import javax.inject.Named
import javax.inject.Qualifier

private val qualifierFilter = { a: Annotation -> a.annotationClass.hasAnnotation<Qualifier>() }

internal fun Array<Annotation>.filterQualifiers() = this.filter(qualifierFilter)

internal fun Iterable<Annotation>.filterQualifiers() = this.filter(qualifierFilter)

internal fun Sequence<Annotation>.filterQualifiers() = this.filter(qualifierFilter)

internal val AnnotatedElement.qualifiers: List<Annotation>
    get() = annotations.asSequence()
        .filterQualifiers()
        .sortedBy { it.annotationClass.qualifiedName }
        .toList()

private fun createStringTag(any: Any): String = when (any) {
    is String -> any
    is Named -> any.value
    else -> any.toString()
}

/** Serializes [Qualifier] annotations or a [String] into a map-friendly key. */
fun createStringTag(list: List<Any>): String? = when (list.size) {
    0 -> null
    1 -> createStringTag(list.single())
    else -> list.joinToString { createStringTag(it) }
}

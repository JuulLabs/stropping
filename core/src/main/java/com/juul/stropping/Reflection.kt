package com.juul.stropping

import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Field
import javax.inject.Named
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.full.findAnnotation

inline fun <reified A : Annotation> Class<*>.fieldsWithAnnotation(): Sequence<Field> {
    val properties = mutableListOf<Field>()
    var clazz: Class<*>? = this
    while (clazz != null) {
        properties += clazz.declaredFields
            .filter { it.isAnnotationPresent(A::class.java) }
        clazz = clazz.superclass
    }
    return properties.asSequence()
}

internal inline fun <reified A: Annotation> KAnnotatedElement.hasAnnotation(): Boolean {
    return findAnnotation<A>() != null
}

internal fun getTag(element: KAnnotatedElement): Any? {
    return element.findAnnotation<Named>()?.value
}

internal fun getTag(element: AnnotatedElement): Any? {
    return element.annotations.filterIsInstance<Named>().firstOrNull()?.value
}

package com.juul.stropping

import java.lang.reflect.Field
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

inline fun <reified A: Annotation> KAnnotatedElement.hasAnnotation(): Boolean {
    return findAnnotation<A>() != null
}

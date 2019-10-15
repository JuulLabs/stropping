package com.juul.stropping

import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Field
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.full.findAnnotation

/**
 * Do something with a [Field], guaranteeing it is accessible during [withAccess]. After the lambda
 * completes, the value of [Field.isAccessible] is returned to whatever the original value was.
 */
internal inline fun <T> Field.access(
    crossinline withAccess: Field.() -> T
): T {
    val wasAccessible = isAccessible
    isAccessible = true
    val blockResult = withAccess.invoke(this)
    isAccessible = wasAccessible
    return blockResult
}

/** Calls [Field.set] while guaranteeing that it accessible during the call. */
internal fun <T> Field.forceSet(
    receiver: Any,
    value: T
) = access {
    set(receiver, value)
}

/** Calls [Field.get] while guaranteeing that it accessible during the call. */
internal fun <T> Field.forceGet(
    receiver: Any
): T = access {
    @Suppress("UNCHECKED_CAST")
    get(receiver) as T
}

internal inline fun <reified A : Annotation> Class<*>.fieldsWithAnnotation(): Sequence<Field> {
    val properties = mutableListOf<Field>()
    var clazz: Class<*>? = this
    while (clazz != null) {
        properties += clazz.declaredFields
            .filter { it.isAnnotationPresent(A::class.java) }
        clazz = clazz.superclass
    }
    return properties.asSequence()
}

internal inline fun <reified A: Annotation> AnnotatedElement.findAnnotation(): A? {
    return annotations.filterIsInstance<A>().firstOrNull()
}

internal inline fun <reified A: Annotation> AnnotatedElement.hasAnnotation(): Boolean {
    return findAnnotation<A>() != null
}

internal inline fun <reified A: Annotation> KAnnotatedElement.hasAnnotation(): Boolean {
    return findAnnotation<A>() != null
}

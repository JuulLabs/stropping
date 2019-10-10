package com.juul.stropping

import java.lang.reflect.Field
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.jvm.isAccessible

fun <T> Any.invokeMethod(methodName: String, vararg args: Any?): T {
    return this::class.declaredMemberFunctions
        .first { it.name == methodName }
        .apply { isAccessible = true }
        .call(this, *args) as T
}

/**
 * If [this] is a direct subtype of [forClass], returns the [KType] of the declaration; otherwise
 * returns `null`.
 */
fun Any.getImplementingKType(forClass: KClass<*>): KType? =
    this::class.supertypes.firstOrNull { it.classifier == forClass }

val KType.argumentTypes: Sequence<KType?>
    get() = this.arguments.asSequence().map { it.type }

val KType.argumentClasses: Sequence<KClass<*>?>
    get() = this.argumentTypes.map { it?.classifier as? KClass<*> }

val KType.argumentClass: KClass<*>
    get() = checkNotNull(this.argumentClasses.single())

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

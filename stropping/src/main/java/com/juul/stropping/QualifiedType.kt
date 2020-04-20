package com.juul.stropping

import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.PRIVATE
import java.lang.reflect.Type
import javax.inject.Named
import javax.inject.Qualifier

/**
 * A [Type] with additional metadata provided from [Qualifier] annotations.
 *
 * @param type The generic type information.
 * @param qualifiers A list of [Qualifier] annotations. Note that the [Named] qualifier can be
 * substituted for a raw string.
 */
data class QualifiedType(
    val type: Type,
    @VisibleForTesting(otherwise = PRIVATE)
    val qualifiers: List<Any>
) {
    init {
        fun Any.isQualifierAnnotation() =
            this is Annotation && this.annotationClass.hasAnnotation<Qualifier>()
        require(qualifiers.all { it is String || it.isQualifierAnnotation() })
        require(qualifiers.count { it is String || it is Named } <= 1)
    }

    /** Returns the qualifiers of this function serialized as a map-friendly [String], or `null`. */
    fun qualifierString(): String? = createStringTag(qualifiers)
}

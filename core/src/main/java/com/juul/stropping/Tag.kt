package com.juul.stropping

import java.lang.reflect.AnnotatedElement
import javax.inject.Named
import kotlin.reflect.KAnnotatedElement

internal fun getTag(annotations: List<Annotation>): Any? =
    annotations.filterIsInstance<Named>().firstOrNull()?.value

internal fun getTag(element: KAnnotatedElement?): Any? =
    getTag(element?.annotations.orEmpty())

internal fun getTag(element: AnnotatedElement?): Any? =
    getTag(element?.annotations?.toList().orEmpty())

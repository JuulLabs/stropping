package com.juul.stropping

import android.util.Log
import org.kodein.di.Kodein
import org.kodein.di.TypeToken
import org.kodein.di.bindings.NoArgSimpleBindingKodein
import org.kodein.di.generic.provider
import org.kodein.di.generic.singleton
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Type
import javax.inject.Singleton
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KType

private fun Kodein.Builder.bind(
    typeToken: TypeToken<Any>,
    annotations: List<Annotation>,
    buildName: String,
    build: NoArgSimpleBindingKodein<*>.() -> Any
) {
    val isSingleton = annotations.any { it is Singleton }
    val (bindingType, binding) = when (isSingleton) {
        true -> "singleton" to singleton { build() }
        false -> "provider" to provider { build() }
    }
    val tag = getTag(annotations)
    Log.d("Stropping", "Binding $bindingType for $typeToken (tag=$tag) with $buildName")
    Bind(typeToken, tag) with binding
}

internal fun Kodein.Builder.bind(
    type: Type,
    element: AnnotatedElement?,
    buildName: String,
    build: NoArgSimpleBindingKodein<*>.() -> Any
) = bind(createTypeToken(type), element?.annotations?.toList().orEmpty(), buildName, build)

internal fun Kodein.Builder.bind(
    type: KType,
    element: KAnnotatedElement?,
    builderName: String,
    build: NoArgSimpleBindingKodein<*>.() -> Any
) = bind(createTypeToken(type), element?.annotations.orEmpty(), builderName, build)

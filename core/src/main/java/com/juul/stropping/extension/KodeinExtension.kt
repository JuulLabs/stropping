package com.juul.stropping.extension

import com.juul.stropping.fieldsWithAnnotation
import dagger.Component
import org.kodein.di.Kodein
import org.kodein.di.TypeToken
import org.kodein.di.direct
import javax.inject.Inject
import kotlin.reflect.KClass
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.primaryConstructor

internal fun Kodein.Builder.importDagger(componentClass: KClass<*>) {
    @UseExperimental(ExperimentalStdlibApi::class)
    require(componentClass.hasAnnotation<Component>())
    require(componentClass.typeParameters.isEmpty())
}

internal fun Kodein.inject(receiver: Any) {
    val typeTokenConstructor = Class.forName("org.kodein.di.ParameterizedTypeToken")
        .kotlin.primaryConstructor!!
    println("Injecting class with type: " + receiver::class.simpleName)
    receiver::class.java.fieldsWithAnnotation<Inject>()
        .forEach { property ->
            @Suppress("UNCHECKED_CAST")
            val typeToken = typeTokenConstructor
                .call(property.type) as TypeToken<Any>
            println("...injecting field with type: " + typeToken.simpleDispString())
            property.forceSet(receiver, direct.Instance(typeToken))
        }
}

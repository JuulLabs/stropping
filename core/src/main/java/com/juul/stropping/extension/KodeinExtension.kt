package com.juul.stropping.extension

import com.juul.stropping.fieldsWithAnnotation
import com.juul.stropping.utility.createTypeToken
import com.juul.stropping.utility.getModulesForComponentClass
import dagger.Binds
import dagger.Component
import dagger.Module
import org.kodein.di.DKodein
import org.kodein.di.Kodein
import org.kodein.di.direct
import org.kodein.di.generic.provider
import org.kodein.di.generic.singleton
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.valueParameters

@UseExperimental(ExperimentalStdlibApi::class)
private fun DKodein.autoInject(type: KType): Any {
    val constructors = (type.classifier as KClass<*>).constructors
    val constructor = constructors.singleOrNull()
        ?: constructors.single { it.hasAnnotation<Inject>() }

    val params = constructor.valueParameters
        .map { Instance(createTypeToken(it.type)) }
        .toTypedArray()
    return constructor.call(*params)
}

/** Imports from either a [Component] or [Module], ignoring submodules & includes. */
@UseExperimental(ExperimentalStdlibApi::class)
private fun Kodein.Builder.importDaggerFunctions(kClass: KClass<*>) {
    for (function in kClass.declaredMemberFunctions) {
        if (function.hasAnnotation<Binds>()) {
            val instanceType = function.valueParameters.single().type
            val bind = Bind(createTypeToken(function.returnType))
            if (function.hasAnnotation<Singleton>()) {
                bind with singleton { autoInject(instanceType) }
            } else {
                bind with provider { autoInject(instanceType) }
            }
        }
    }
}

internal fun Kodein.Builder.importDaggerComponent(componentClass: KClass<*>) {
    importDaggerFunctions(componentClass)
    val modules = getModulesForComponentClass(componentClass)
    for (module in modules) {
        importDaggerFunctions(module)
    }
}

internal fun Kodein.inject(receiver: Any) {
    receiver::class.java.fieldsWithAnnotation<Inject>()
        .forEach { field ->
            field.forceSet(receiver, direct.Instance(createTypeToken(field.type)))
        }
}

package com.juul.stropping

import android.app.Application
import dagger.android.DispatchingAndroidInjector
import java.lang.reflect.Type
import kotlin.reflect.KClass

interface Engine {

    fun <T : Any> overwrite(type: Type, value: T, named: String?)

    fun <K : Any, V : Any> addIntoMap(
        keyType: Type,
        key: K,
        valueType: Type,
        value: V,
        named: String?
    )

    fun <T : Any> getInstanceOf(qualifiedType: QualifiedType): T

    fun <T : Any> inject(injectable: Injectable<T>): T {
        return injectable.inject { qualifiedType -> getInstanceOf(qualifiedType) }
    }

    interface Builder {
        fun build(
            context: Application,
            graph: Graph,
            androidInjector: DispatchingAndroidInjector<*>
        ): Engine
    }
}

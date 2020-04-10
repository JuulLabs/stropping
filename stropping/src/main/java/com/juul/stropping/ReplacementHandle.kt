package com.juul.stropping

import android.app.Application
import dagger.android.DaggerApplication
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.multibindings.IntoMap
import java.lang.reflect.Type
import javax.inject.Named
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSupertypeOf
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.javaField

/** A wrapper around the replacement [DispatchingAndroidInjector] with graph mutator methods. */
abstract class ReplacementHandle {

    /** Gets the [androidInjector] which does the deeds. */
    internal abstract val androidInjector: DispatchingAndroidInjector<*>

    /** Gets the component which does the deeds. */
    internal abstract val component: Any

    /** Installs [component] and [androidInjector] inside the [application]. */
    internal fun install(application: Application) {
        installAndroidInjector(application)
        installComponent(application)
    }

    private fun installAndroidInjector(application: Application) = when (application) {
        is DaggerApplication -> {
            DaggerApplication::class.java.getDeclaredField("androidInjector")
                .forceSet(application, androidInjector)
        }
        is HasAndroidInjector -> TODO("Not sure if possible?")
        else -> TODO("Not sure if possible?")
    }

    private fun installComponent(application: Application) {
        val field = application::class.declaredMemberProperties
            .firstOrNull { it.returnType.isSupertypeOf(component::class.starProjectedType) }
        if (field != null) {
            val delegate = application::class.java.declaredFields
                .firstOrNull { it.name == "${field.name}\$delegate" }
            val javaField = field.javaField
            when {
                delegate != null -> delegate.forceSet(application, lazy { component })
                javaField != null -> javaField.forceSet(application, component)
                else -> TODO("Not sure if possible?")
            }
        }
    }

    /**
     * Replaces the provision method in Dagger for type [T] with either no qualifiers, or a [Named]
     * qualifier, with one that provides the given [value].
     */
    inline fun <reified T : Any> overwrite(
        value: T,
        named: String? = null
    ) = overwrite(javaTypeOf<T>(), value, named)

    /** Provides a value from the _current_ dependency graph. */
    inline fun <reified T : Any> provide(
        named: String? = null
    ): T = provide(javaTypeOf<T>(), named)

    /**
     * Replaces the provision method in Dagger for type [V] with either no qualifiers, or a [Named]
     * qualifier, with one the provides the given [value] and is annotated with [IntoMap]. The map
     * key is faked as one with a return type of [K] and a value of [key].
     */
    inline fun <reified K : Any, reified V : Any> addIntoMap(
        key: K,
        value: V,
        named: String? = null
    ) = addIntoMap(javaTypeOf<K>(), key, javaTypeOf<V>(), value, named)

    abstract fun <T : Any> overwrite(
        type: Type,
        value: T,
        named: String?
    )

    /** Provides a value from the _current_ dependency graph. [T] _must_ be assignable from [type]. */
    abstract fun <T : Any> provide(
        type: Type,
        named: String?
    ): T

    /** Implementation detail of `addIntoMap<K, V>`. You likely don't want to call this directly. */
    abstract fun <K : Any, V : Any> addIntoMap(
        keyType: Type,
        key: K,
        valueType: Type,
        value: V,
        named: String?
    )
}

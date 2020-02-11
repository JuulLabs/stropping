package com.juul.stropping

import android.app.Application
import dagger.android.DaggerApplication
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.multibindings.IntoMap
import java.lang.reflect.Type
import javax.inject.Named

/** A wrapper around the replacement [DispatchingAndroidInjector] with graph mutator methods. */
abstract class ReplacementHandle {

    /** Gets the [androidInjector] which does the deeds. */
    internal abstract val androidInjector: DispatchingAndroidInjector<*>

    /**
     * Installs [androidInjector] inside the [application].
     *
     * TODO: Right now this only supports [DaggerApplication]s.
     */
    internal fun install(application: Application) = when (application) {
        is DaggerApplication -> {
            DaggerApplication::class.java.getDeclaredField("androidInjector")
                .forceSet(application, androidInjector)
        }
        is HasAndroidInjector -> TODO("Not sure if possible?")
        else -> TODO("Not sure if possible?")
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

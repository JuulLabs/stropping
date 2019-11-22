package com.juul.stropping

import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.PROTECTED
import com.juul.stropping.common.forceSet
import com.juul.stropping.common.hasAnnotation
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import javax.inject.Inject
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod

/** Base class providing an abstraction for the different kinds of things that can be injected. */
abstract class Injectable<T> {
    /** List of [QualifiedType] parameters to be passed to the [inject] call's injector. */
    @VisibleForTesting(otherwise = PROTECTED)
    abstract val parameters: List<QualifiedType>

    /** Call this injectable, and return the result (if any). */
    protected abstract fun applyInjected(values: List<Any?>): T

    /** Perform injection on this. The lambda [injector] is called for each value that needs to be provided. */
    fun inject(injector: (QualifiedType) -> Any?): T {
        return applyInjected(parameters.map(injector))
    }
}

/**
 * A wrapper around a [Constructor] for injection.
 *
 * @constructor Create an instance from an explicit constructor. Usually, use [InjectableConstructor.fromClass] instead.
 */
class InjectableConstructor<T : Any>(
    private val constructor: Constructor<T>
) : Injectable<T>() {
    companion object {
        /**
         * Create an instance of an [InjectableConstructor] for class [T]. Returns `null` if there
         * are multiple constructors and not exactly-one of them are annotated with [Inject].
         */
        inline fun <reified T : Any> fromClass(): Injectable<T>? {
            return fromClass(T::class.java)
        }

        /**
         * Create an instance of an [InjectableConstructor] for [clazz]. Returns `null` if there
         * are multiple constructors and not exactly-one of them are annotated with [Inject].
         */
        fun <T : Any> fromClass(clazz: Class<T>): Injectable<T>? {
            val constructor = clazz.constructors.singleOrNull()
                ?: clazz.constructors.singleOrNull { it.hasAnnotation<Inject>() }
                ?: return null
            @Suppress("UNCHECKED_CAST")
            return InjectableConstructor(constructor as Constructor<T>)
        }
    }

    override val parameters: List<QualifiedType> by lazy {
        (constructor.genericParameterTypes zip constructor.parameterAnnotations)
            .map { (type, annotations) -> QualifiedType(type, annotations.filterQualifiers()) }
    }

    override fun applyInjected(values: List<Any?>): T {
        return constructor.newInstance(*values.toTypedArray())
    }
}

/**
 * Injectable for instance types, such as an Android `Activity`.
 *
 * @constructor Create an injectable instance for a value.
 */
class InjectableInstance(
    private val instance: Any
) : Injectable<Unit>() {

    private val injectableFields: List<Field> by lazy {
        instance::class.java.fields.filter { it.hasAnnotation<Inject>() }
    }

    override val parameters: List<QualifiedType> by lazy {
        injectableFields.map { QualifiedType(it.genericType, it.qualifiers) }
    }

    override fun applyInjected(values: List<Any?>) {
        for ((field, value) in injectableFields zip values) {
            field.forceSet(instance, value)
        }
    }
}

/**
 * Injectable for methods, including the receiver of the method call.
 *
 * @constructor Create an instance of the injectable method.
 */
class InjectableMethod<T>(
    private val receiver: Any,
    private val method: Method
) : Injectable<T>() {

    /** Create an instance of an injectable method, from a Kotlin function. */
    constructor(receiver: Any, kFunction: KFunction<T>) : this(receiver, kFunction.javaMethod!!)

    init {
        require(method.declaringClass == receiver::class.java)
        require(!Modifier.isStatic(method.modifiers))
    }

    override val parameters: List<QualifiedType> by lazy {
        (method.genericParameterTypes zip method.parameterAnnotations)
            .map { (type, annotations) -> QualifiedType(type, annotations.filterQualifiers()) }
    }

    override fun applyInjected(values: List<Any?>): T {
        @Suppress("UNCHECKED_CAST")
        return method.invoke(receiver, *values.toTypedArray()) as T
    }
}

/**
 * Injectable for static methods with no receiver.
 *
 * @constructor Create an instance of the injectable static method.
 */
class InjectableStaticMethod<T>(
    private val method: Method
) : Injectable<T>() {
    init {
        require(Modifier.isStatic(method.modifiers))
    }

    override val parameters: List<QualifiedType> by lazy {
        (method.genericParameterTypes zip method.parameterAnnotations)
            .map { (type, annotations) -> QualifiedType(type, annotations.filterQualifiers()) }
    }

    override fun applyInjected(values: List<Any?>): T {
        @Suppress("UNCHECKED_CAST")
        return method.invoke(null, *values.toTypedArray()) as T
    }
}

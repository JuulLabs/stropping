package com.juul.stropping

import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.PRIVATE
import androidx.annotation.VisibleForTesting.PROTECTED
import dagger.Binds
import dagger.Provides
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Type
import javax.inject.Qualifier
import javax.inject.Singleton

/** Abstraction around Dagger [provision methods](https://dagger.dev/api/latest/dagger/Component.html#provision-methods). */
sealed class Provisioner {

    /** Raw return type. Used to supply [qualifiedType]. */
    @VisibleForTesting(otherwise = PROTECTED)
    abstract val returnType: Type

    /** Multibinding information. */
    abstract val multibindings: Multibindings

    /** Whether or not this provisioner is a singleton. */
    abstract val isSingleton: Boolean

    /** Return type with [Qualifier] information. */
    abstract val qualifiedType: QualifiedType

    companion object {
        /** Creates a [MethodProvisioner] from a method reference, if possible. */
        fun fromMethod(method: Method): Provisioner? = when {
            method.typeParameters.isNotEmpty() -> null
            method.hasAnnotation<Binds>() -> BindsMethodProvisioner(method)
            method.hasAnnotation<Provides>() -> ProvidesMethodProvisioner(method)
            else -> null
        }
    }
}

/**
 * A [Provisioner] for a pre-defined instance. This is not a concept in Dagger, but is useful for
 * our `kodein` module.
 *
 * @param value The instance value for this [Provisioner].
 */
class ValueProvisioner(
    val value: Any,
    override val returnType: Type,
    named: String?,
    override val multibindings: Multibindings
) : Provisioner() {
    override val isSingleton: Boolean = true
    override val qualifiedType: QualifiedType = QualifiedType(returnType, listOfNotNull(named))
}

/**
 * A [Provisioner] for a Dagger method.
 *
 * @param method The [Method] which this represents.
 */
sealed class MethodProvisioner(
    val method: Method
) : Provisioner() {

    init {
        require(method.typeParameters.isEmpty())
    }

    /** The class which [method] is declared in. */
    val declaringClass: Class<*> = method.declaringClass

    override val returnType: Type = method.genericReturnType

    /** [Qualifier]s of this method. Used to supply [qualifiedType]. */
    @VisibleForTesting(otherwise = PRIVATE)
    val qualifiers: List<Annotation> = method.qualifiers

    override val multibindings: Multibindings = Multibindings.fromAnnotations(method)

    override val isSingleton: Boolean = method.hasAnnotation<Singleton>()

    /** Parameters for this method. */
    val parameters: List<QualifiedType>
        get() = (method.genericParameterTypes zip method.parameterAnnotations)
            .map { (type, annotations) -> QualifiedType(type, annotations.filterQualifiers()) }

    override val qualifiedType: QualifiedType
        get() = QualifiedType(returnType, qualifiers)
}

/** Instance of [MethodProvisioner] for [Binds] annotated methods. */
class BindsMethodProvisioner(
    method: Method
) : MethodProvisioner(method) {
    init {
        require(method.hasAnnotation<Binds>())
        require(parameters.size == 1)
    }
}

/** Instance of [MethodProvisioner] for [Provides] annotated methods. */
class ProvidesMethodProvisioner(
    method: Method
) : MethodProvisioner(method) {
    init {
        require(method.hasAnnotation<Provides>())
    }

    val isStatic = Modifier.isStatic(method.modifiers)
}

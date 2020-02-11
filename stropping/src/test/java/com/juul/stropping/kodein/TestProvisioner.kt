package com.juul.stropping

import dagger.Provides
import dagger.multibindings.IntoMap
import dagger.multibindings.IntoSet
import dagger.multibindings.StringKey
import org.junit.Test
import java.lang.reflect.ParameterizedType
import javax.inject.Named
import javax.inject.Qualifier
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private const val EXAMPLE_NAME = "example"
private const val EXAMPLE_KEY = "key"

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
private annotation class CustomQualifier

private class Functions {
    @Provides
    @CustomQualifier
    fun provideCustomQualifiedString(): String = "custom-qualified string"

    @Provides
    @Named(EXAMPLE_NAME)
    @CustomQualifier
    fun provideCustomQualifiedNamedString(): String = "custom-qualified, named string"

    @Provides
    fun provideGenericType(): List<String> = listOf("string inside a list")

    @Provides
    @Named(EXAMPLE_NAME)
    fun provideNamedString(): String = "named string"

    @Provides
    @IntoSet
    fun provideIntoSet(): String = "string in set"

    @Provides
    @IntoMap
    @StringKey(EXAMPLE_KEY)
    fun provideIntoMap(): String = "string in map"

    @Provides
    fun provideFromParameter(string: String): String =
        "from param: $string"

    @Provides
    fun provideFromGenericParameter(list: List<String>): String =
        "from list param: ${list.joinToString()}"

    @Provides
    fun provideFromMultipleParameters(
        string: String,
        number: Any
    ): String = "from params: $string and $number"
}

class TestProvisioner {

    private fun getProvisionFunction(function: KFunction<*>): MethodProvisioner {
        val javaMethod = assertNotNull(function.javaMethod)
        val provisioner = assertNotNull(Provisioner.fromMethod(javaMethod))
        return assertIsInstance<MethodProvisioner>(provisioner)
    }

    @Test
    fun `declaringClass works`() {
        val provisionFunction = getProvisionFunction(Functions::provideNamedString)
        assertEquals(Functions::class.java, provisionFunction.declaringClass)
    }

    @Test
    fun `multibindings works for single`() {
        val provisionFunction = getProvisionFunction(Functions::provideNamedString)
        assertEquals(Multibindings.Single, provisionFunction.multibindings)
    }

    @Test
    fun `multibindings works for set`() {
        val provisionFunction = getProvisionFunction(Functions::provideIntoSet)
        assertEquals(Multibindings.ToSet, provisionFunction.multibindings)
    }

    @Test
    fun `multibindings works for map`() {
        val provisionFunction = getProvisionFunction(Functions::provideIntoMap)
        assertIsInstance<Multibindings.ToMap>(provisionFunction.multibindings)
        val toMap = provisionFunction.multibindings as Multibindings.ToMap
        assertEquals(String::class.java, toMap.keyType)
        assertEquals(EXAMPLE_KEY, toMap.keyValue)
    }

    @Test
    fun `parameters works with class`() {
        val provisionFunction = getProvisionFunction(Functions::provideFromParameter)
        val parameters = provisionFunction.parameters
        assertEquals(String::class.java, parameters.single().type)
    }

    @Test
    fun `parameters works with generics`() {
        val provisionFunction = getProvisionFunction(Functions::provideFromGenericParameter)
        val parameter = provisionFunction.parameters.single()
        assertIsInstance<ParameterizedType>(parameter.type)
        val type = parameter.type as ParameterizedType
        assertEquals(List::class.java, type.rawType)
        assertEquals(String::class.java, type.actualTypeArguments.single())
    }

    @Test
    fun `parameters works with multiple`() {
        val provisionFunction = getProvisionFunction(Functions::provideFromMultipleParameters)
        val parameters = provisionFunction.parameters
        assertEquals(String::class.java, parameters[0].type)
        assertEquals(Any::class.java, parameters[1].type)
    }

    @Test
    fun `qualifiers recognizes @Named`() {
        val provisionFunction = getProvisionFunction(Functions::provideNamedString)
        val named = provisionFunction.qualifiers.single() as Named
        assertEquals(EXAMPLE_NAME, named.value)
    }

    @Test
    fun `qualifiers recognizes custom qualifiers`() {
        val provisionFunction = getProvisionFunction(Functions::provideCustomQualifiedString)
        assertIsInstance<CustomQualifier>(provisionFunction.qualifiers.single())
    }

    @Test
    fun `qualifiers recognizes multiple qualifiers`() {
        val provisionFunction = getProvisionFunction(Functions::provideCustomQualifiedNamedString)
        assertEquals(2, provisionFunction.qualifiers.size)
        assertEquals(1, provisionFunction.qualifiers.count { it is Named })
        assertEquals(1, provisionFunction.qualifiers.count { it is CustomQualifier })
    }

    @Test
    fun `returnType works for classes`() {
        val provisionFunction = getProvisionFunction(Functions::provideNamedString)
        assertEquals(String::class.java, provisionFunction.returnType)
    }

    @Test
    fun `returnType works for parameterized types`() {
        val provisionFunction = getProvisionFunction(Functions::provideGenericType)
        assertIsInstance<ParameterizedType>(provisionFunction.returnType)
        val type = provisionFunction.returnType as ParameterizedType
        assertEquals(List::class.java, type.rawType)
        assertEquals(String::class.java, type.actualTypeArguments.single())
    }
}

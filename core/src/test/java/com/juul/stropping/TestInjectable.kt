@file:Suppress("UNUSED_PARAMETER")

package com.juul.stropping

import org.junit.Test
import javax.inject.Inject
import javax.inject.Named
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TestInjectableConstructor {

    @Test
    fun `InjectableConstructor from @Inject constructor`() {
        class PseudoManager @Inject constructor(field: String) {
            constructor() : this("")
        }

        val constructor = InjectableConstructor.fromClass<PseudoManager>()
        assertNotNull(constructor)
        val expectedType = QualifiedType(String::class.java, emptyList())
        assertEquals(expectedType, constructor.parameters.single())
    }

    @Test
    fun `InjectableConstructor from multiple @Inject constructors is null`() {
        class PseudoManager @Inject constructor() {
            @Inject
            constructor(field: String) : this()
        }
        assertNull(InjectableConstructor.fromClass<PseudoManager>())
    }

    @Test
    fun `InjectableConstructor from multiple non-@Inject constructors is null`() {
        class PseudoManager() {
            constructor(field: String) : this()
        }
        assertNull(InjectableConstructor.fromClass<PseudoManager>())
    }

    @Test
    fun `InjectableConstructor from single constructor`() {
        class PseudoManager(field: String)

        val constructor = InjectableConstructor.fromClass<PseudoManager>()
        assertNotNull(constructor)
        val expectedType = QualifiedType(String::class.java, emptyList())
        assertEquals(expectedType, constructor.parameters.single())
    }
}

class TestInjectableInstance {

    @Test
    fun `InjectableInstance works`() {
        class PseudoActivity {
            @Inject
            lateinit var field: String
        }

        val instance = PseudoActivity()
        val injectable = InjectableInstance(instance)
        val expectedType = QualifiedType(String::class.java, emptyList())
        assertEquals(expectedType, injectable.parameters.single())
        val injectedString = "injected"
        injectable.inject { injectedString }
        assertEquals(injectedString, instance.field)
    }

    // TODO: More testing would be prudent
}

class TestInjectableMethod {

    @Test
    fun `TestInjectableMethod works`() {
        class PseudoModule {
            fun provideUri(
                @Named("proto") protocol: String,
                @Named("url") baseUrl: String
            ): String = "$protocol://$baseUrl"
        }

        val instance = PseudoModule()
        val injectable = InjectableMethod(instance, instance::provideUri)

        val returnedValue = injectable.inject { qualifiedType ->
            val named = qualifiedType.qualifiers.filterIsInstance<Named>().singleOrNull()
            when (named?.value) {
                "proto" -> "https"
                "url" -> "example.com"
                else -> null
            }
        }
        assertEquals("https://example.com", returnedValue)
    }

    // TODO: More testing would be prudent
}

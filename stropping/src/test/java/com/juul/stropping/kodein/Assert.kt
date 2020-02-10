package com.juul.stropping

import kotlin.test.assertTrue

inline fun <reified T : Any> assertIsInstance(actual: Any, message: String? = null) {
    val msg = message
        ?: "Expected instance of ${T::class.simpleName}. Got ${actual::class.simpleName} instead."
    assertTrue(msg) { actual is T }
}

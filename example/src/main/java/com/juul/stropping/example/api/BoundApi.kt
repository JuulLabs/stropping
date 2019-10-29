package com.juul.stropping.example.api

import javax.inject.Inject

interface BoundApi {
    fun getValue(): String
}

class ConstantBoundApi @Inject constructor() : BoundApi {
    override fun getValue(): String =
        "Text from Dagger-injected ${ConstantBoundApi::class.java.simpleName}"
}

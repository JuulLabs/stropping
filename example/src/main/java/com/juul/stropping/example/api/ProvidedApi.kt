package com.juul.stropping.example.api

import javax.inject.Inject

interface ProvidedApi {
    fun getValue(): String
}

class ConstantProvidedApi @Inject constructor() : ProvidedApi {
    override fun getValue(): String =
        "Text from Dagger-injected ${ConstantProvidedApi::class.java.simpleName}"
}

package com.juul.stropping.example.api

import javax.inject.Inject

class ConstantSimpleApi @Inject constructor() : SimpleApi {
    override fun getValue(): String = "Text from ${ConstantSimpleApi::class.java.simpleName}"
}

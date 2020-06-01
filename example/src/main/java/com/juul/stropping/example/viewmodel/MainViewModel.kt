package com.juul.stropping.example.viewmodel

import android.app.Application
import com.juul.stropping.example.api.BASE_URL_NAME
import com.juul.stropping.example.api.BoundApi
import com.juul.stropping.example.api.NamedParametersApi
import com.juul.stropping.example.api.ProvidedApi
import javax.inject.Inject
import javax.inject.Named

class MainViewModel(
    application: Application
) : DaggerViewModel(application) {

    @Inject
    lateinit var boundApi: BoundApi

    @Inject
    lateinit var providedApi: ProvidedApi

    @Inject
    lateinit var namedParametersApi: NamedParametersApi

    @Inject
    @Named(BASE_URL_NAME)
    lateinit var baseUrl: String

    val displayText: String = listOf(
        boundApi.getValue(),
        providedApi.getValue(),
        "baseUrl: " + namedParametersApi.baseUrl,
        "userAgent: " + namedParametersApi.userAgent
    ).joinToString(separator = "\n")
}

package com.juul.stropping.example.viewmodel

import android.app.Application
import com.juul.stropping.example.api.BoundApi
import com.juul.stropping.example.api.ProvidedApi
import javax.inject.Inject

class MainViewModel(
    application: Application
) : DaggerViewModel(application) {

    @Inject
    lateinit var boundApi: BoundApi

    @Inject
    lateinit var providedApi: ProvidedApi

    val displayText: String = "${boundApi.getValue()}\n${providedApi.getValue()}"
}

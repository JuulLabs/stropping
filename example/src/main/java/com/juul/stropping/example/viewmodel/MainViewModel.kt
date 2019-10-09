package com.juul.stropping.example.viewmodel

import android.app.Application
import com.juul.stropping.example.api.SimpleApi
import javax.inject.Inject

class MainViewModel(
    application: Application
) : DaggerViewModel(application) {

    @Inject
    protected lateinit var simpleApi: SimpleApi

    val displayText: String = simpleApi.getValue()
}

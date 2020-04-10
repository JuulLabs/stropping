package com.juul.stropping.example

import dagger.android.DaggerApplication

class Application : DaggerApplication() {

    private val component: Component by lazy {
        DaggerComponent.builder()
            .application(this)
            .build()
    }

    override fun applicationInjector() = component
}

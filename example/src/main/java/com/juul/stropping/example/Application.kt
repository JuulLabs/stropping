package com.juul.stropping.example

import dagger.android.DaggerApplication

class Application : DaggerApplication() {
    override fun applicationInjector() = DaggerComponent.builder()
        .application(this)
        .build()
}

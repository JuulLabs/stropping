package com.juul.stropping.example.viewmodel

import androidx.lifecycle.AndroidViewModel
import dagger.android.AndroidInjector
import dagger.android.HasAndroidInjector

abstract class DaggerViewModel(
    application: android.app.Application
) : AndroidViewModel(application), HasAndroidInjector {
    final override fun androidInjector(): AndroidInjector<Any> =
        getApplication<com.juul.stropping.example.Application>().androidInjector()

    init {
        @Suppress("LeakingThis")
        androidInjector().inject(this)
    }
}

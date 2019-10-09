package com.juul.stropping.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject

abstract class DaggerViewModel(
    application: Application
) : AndroidViewModel(application), HasAndroidInjector {
    @Inject
    protected lateinit var androidInjector: DispatchingAndroidInjector<Any>

    override fun androidInjector() = androidInjector

    init {
        (application as HasAndroidInjector).androidInjector().inject(this)
    }
}

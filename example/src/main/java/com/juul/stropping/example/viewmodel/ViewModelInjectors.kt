package com.juul.stropping.example.viewmodel

import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
interface ViewModelInjectors {
    @ContributesAndroidInjector
    fun contributeMainViewModelInjector(): MainViewModel
}

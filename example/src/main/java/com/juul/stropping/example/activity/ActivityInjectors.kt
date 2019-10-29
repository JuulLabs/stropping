package com.juul.stropping.example.activity

import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
interface ActivityInjectors {
    @ContributesAndroidInjector
    fun contributeMainActivityInjector(): MainActivity
}

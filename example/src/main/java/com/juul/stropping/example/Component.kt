package com.juul.stropping.example

import com.juul.stropping.example.activity.ActivityInjectors
import com.juul.stropping.example.api.ApiBindings
import com.juul.stropping.example.api.ApiProviders
import com.juul.stropping.example.viewmodel.ViewModelInjectors
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        // System modules
        AndroidSupportInjectionModule::class,
        // Application modules
        ActivityInjectors::class,
        ApiBindings::class,
        ApiProviders::class,
        ViewModelInjectors::class
    ]
)
interface Component : AndroidInjector<Application> {
    @Component.Builder
    interface Builder {
        fun build(): com.juul.stropping.example.Component
        @BindsInstance
        fun application(application: Application): Builder
    }
}

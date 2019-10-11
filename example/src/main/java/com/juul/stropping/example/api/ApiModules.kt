package com.juul.stropping.example.api

import dagger.Binds
import dagger.Module
import dagger.Provides

@Module
abstract class ApiBindings {
    @Binds
    abstract fun bindBoundApi(constantBoundApi: ConstantBoundApi): BoundApi
}

@Module
class ApiProviders {
    @Provides
    fun provideProvidedApi(): ProvidedApi {
        return ConstantProvidedApi()
    }
}

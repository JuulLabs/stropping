package com.juul.stropping.example.api

import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Named

const val BASE_URL_NAME = "BaseUrl"
const val USER_AGENT_NAME = "UserAgent"

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

    @Provides
    @Named(BASE_URL_NAME)
    fun provideBaseUrl(): String = "http://example.com"

    @Provides
    @Named(USER_AGENT_NAME)
    fun provideUserAgent(): String = "Chrome, Probably"
}

package com.juul.stropping.example.api

import dagger.Binds
import dagger.Module

@Module
abstract class ApiBindings {
    @Binds
    abstract fun bindSimpleApi(constantSimpleApi: ConstantSimpleApi): SimpleApi
}

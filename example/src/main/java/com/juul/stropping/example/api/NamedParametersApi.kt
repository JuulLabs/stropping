package com.juul.stropping.example.api

import javax.inject.Inject
import javax.inject.Named

class NamedParametersApi @Inject constructor(
    @Named(BASE_URL_NAME) val baseUrl: String,
    @Named(USER_AGENT_NAME) val userAgent: String
)

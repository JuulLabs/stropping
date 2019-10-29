package com.juul.stropping.example.viewmodel

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

inline fun <reified T : ViewModel> FragmentActivity.viewModel() =
    object : ReadOnlyProperty<FragmentActivity, T> {
        override fun getValue(thisRef: FragmentActivity, property: KProperty<*>): T {
            return ViewModelProviders.of(thisRef).get(T::class.java)
        }
    }

package com.juul.stropping.example.activity

import android.os.Bundle
import com.juul.stropping.example.R
import com.juul.stropping.example.viewmodel.MainViewModel
import com.juul.stropping.example.viewmodel.viewModel
import dagger.android.support.DaggerAppCompatActivity
import kotlinx.android.synthetic.main.activity_main.mainText

class MainActivity : DaggerAppCompatActivity() {

    private val viewModel by viewModel<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mainText.text = viewModel.displayText
    }
}

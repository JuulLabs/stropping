package com.juul.stropping.example

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.juul.stropping.Replacements
import com.juul.stropping.example.activity.MainActivity
import com.juul.stropping.example.api.SimpleApi
import com.juul.stropping.mockk.addMockk
import io.mockk.every
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ViewModelInjectionTests {
    @Test
    fun replaceSimpleApiText() {
        Replacements.of<Component> {
            addMockk<SimpleApi> {
                every { getValue() } returns "Mocked Value"
            }
        }
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(5000)
        scenario.close()
    }
}

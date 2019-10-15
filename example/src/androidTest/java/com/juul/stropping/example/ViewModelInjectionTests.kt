package com.juul.stropping.example

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.juul.stropping.Replacements
import com.juul.stropping.example.activity.MainActivity
import com.juul.stropping.example.api.BASE_URL_NAME
import com.juul.stropping.example.api.BoundApi
import com.juul.stropping.example.api.ProvidedApi
import com.juul.stropping.example.api.USER_AGENT_NAME
import com.juul.stropping.mockk.overwriteWithMockK
import io.mockk.every
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ViewModelInjectionTests {

    @Test
    fun useOriginalDependencyGraph() {
        Replacements.of<Component> { reset() }
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(1000)
        scenario.close()
    }

    @Test
    fun replaceBoundApi() {
        Replacements.of<Component> {
            reset()
            overwriteWithMockK<BoundApi> {
                every { getValue() } returns "Test From Kodein-injected MockK"
            }
        }
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(1000)
        scenario.close()
    }

    @Test
    fun replaceProvidedApi() {
        Replacements.of<Component> {
            reset()
            overwriteWithMockK<ProvidedApi> {
                every { getValue() } returns "Test From Kodein-injected MockK"
            }
        }
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(1000)
        scenario.close()
    }

    @Test
    fun replaceNamedParametersApi() {
        Replacements.of<Component> {
            reset()
            overwrite("http://injected-with-kodein.com", named = BASE_URL_NAME)
            overwrite("Injected user agent", named = USER_AGENT_NAME)
        }
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(1000)
        scenario.close()
    }
}

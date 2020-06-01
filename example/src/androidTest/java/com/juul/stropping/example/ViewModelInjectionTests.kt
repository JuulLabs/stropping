package com.juul.stropping.example

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.juul.stropping.Replacements
import com.juul.stropping.example.activity.MainActivity
import com.juul.stropping.example.api.BASE_URL_NAME
import com.juul.stropping.example.api.BoundApi
import com.juul.stropping.example.api.ProvidedApi
import com.juul.stropping.example.api.USER_AGENT_NAME
import com.juul.stropping.overwriteWithMockK
import com.juul.stropping.overwriteWithSpyK
import io.mockk.every
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ViewModelInjectionTests {

    @Test
    fun useOriginalDependencyGraph() {
        Replacements.of<Component> {}
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(1000)
        scenario.onActivity { activity ->
            // Expect the member-injected baseUrl to match the constructor-injected baseUrl
            assertEquals(activity.viewModel.namedParametersApi.baseUrl, activity.viewModel.baseUrl)
        }
        scenario.close()
    }

    @Test
    fun replaceBoundApi() {
        Replacements.of<Component> {
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
            overwriteWithMockK<ProvidedApi> {
                every { getValue() } returns "Test From Kodein-injected MockK"
            }
        }
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(1000)
        scenario.close()
    }

    @Test
    fun replaceWithSpyk() {
        Replacements.of<Component> {
            overwriteWithSpyK<ProvidedApi> {
                val original = getValue()
                every { getValue() } returns original.toUpperCase()
            }
        }
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(1000)
        scenario.close()
    }

    @Test
    fun replaceNamedParametersApi() {
        Replacements.of<Component> {
            overwrite("http://injected-with-kodein.com", named = BASE_URL_NAME)
            overwrite("Injected user agent", named = USER_AGENT_NAME)
        }
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(1000)
        scenario.close()
    }
}

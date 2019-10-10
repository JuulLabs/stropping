package com.juul.stropping

import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry
import com.juul.stropping.extension.forceGet
import com.juul.stropping.extension.forceSet
import com.juul.stropping.extension.importDagger
import com.juul.stropping.extension.inject
import dagger.android.DaggerApplication
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import io.mockk.every
import io.mockk.mockk
import org.kodein.di.Kodein
import org.kodein.di.android.x.androidXModule
import org.kodein.di.conf.ConfigurableKodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import kotlin.reflect.KClass

object Replacements {
    private val application: Application by lazy {
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
    }

    private val componentClassToReplacementHandle = mutableMapOf<KClass<*>, ReplacementHandle>()

    inline fun <reified T : Any> of(
        noinline configureReplacements: ReplacementHandle.() -> Unit
    ) = of(T::class, configureReplacements)

    fun <T : Any> of(
        componentClass: KClass<T>,
        configureReplacements: ReplacementHandle.() -> Unit
    ) {
        val handle = componentClassToReplacementHandle.getOrPut(componentClass) {
            createReplacementHandle(componentClass)
        }
        configureReplacements(handle)
    }

    private fun <T : Any> createReplacementHandle(
        componentClass: KClass<T>
    ): ReplacementHandle {
        return ReplacementHandle(application, componentClass)
    }
}

class ReplacementHandle(
    application: Application,
    componentClass: KClass<*>
) {
    /** Proxy dispatching android injector. */
    val proxyInjector = mockk<DispatchingAndroidInjector<Any>> {
        every { inject(any()) } answers { call ->
            kodein.inject(call.invocation.args.first()!!)
        }
    }

    /** Source application graph, created through reflection on the component class. */
    private val applicationGraph = Kodein {
        import(androidXModule(application))
        importDagger(componentClass)
        bind<DispatchingAndroidInjector<Any>>() with instance(proxyInjector)
    }

    /** Mutable kodein graph. */
    private val kodein = ConfigurableKodein(mutable = true)

    init {
        reset()
        when (application) {
            is DaggerApplication -> replaceAndroidInjector(application)
            is HasAndroidInjector -> TODO("Not sure if possible?")
            else -> TODO("Not sure if possible?")
        }
    }

    private fun replaceAndroidInjector(application: DaggerApplication) {
        DaggerApplication::class.java.getDeclaredField("androidInjector")
            .forceSet(application, proxyInjector)
    }

    fun reset() {
        kodein.clear()
        kodein.addExtend(applicationGraph)
    }

    inline fun <reified T : Any> overwrite(
        value: T
    ) {
        val kodein = this::class.java.getDeclaredField("kodein")
            .forceGet<ConfigurableKodein>(this)
        kodein.addConfig {
            bind<T>() with instance(value)
        }
    }
}

package com.juul.stropping

import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry
import com.juul.stropping.kodein.KodeinEngine
import dagger.android.DaggerApplication
import kotlin.reflect.KClass

/** The default engine builder. */
val DEFAULT_ENGINE_BUILDER: Engine.Builder = KodeinEngine.Builder()

/**
 * Entry point for replacing a [DaggerApplication]'s injector during runtime.
 *
 * @see EngineReplacementHandle
 */
object Replacements {
    private val application: Application
        get() = InstrumentationRegistry.getInstrumentation()
            .targetContext
            .applicationContext as Application

    private val componentClassToReplacementHandle = mutableMapOf<KClass<*>, EngineReplacementHandle>()

    /** Gets and configures a [EngineReplacementHandle] for the component of type [T]. */
    inline fun <reified T : Any> of(
        resetConfiguration: Boolean = true,
        engineBuilder: Engine.Builder = DEFAULT_ENGINE_BUILDER,
        noinline configureReplacements: EngineReplacementHandle.() -> Unit
    ) = of(T::class, resetConfiguration, engineBuilder, configureReplacements)

    /** Gets and configures a [EngineReplacementHandle] for the component of type [T]. */
    fun <T : Any> of(
        componentClass: KClass<T>,
        resetConfiguration: Boolean = true,
        engineBuilder: Engine.Builder = DEFAULT_ENGINE_BUILDER,
        configureReplacements: EngineReplacementHandle.() -> Unit
    ) {
        val existing = componentClassToReplacementHandle[componentClass]
        val handle = when {
            existing == null || resetConfiguration ->
                EngineReplacementHandle(application, componentClass, engineBuilder)
            else -> existing
        }
        configureReplacements(handle)
        handle.install(application)
    }
}

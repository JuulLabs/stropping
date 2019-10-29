package com.juul.stropping.kodein

import android.app.Application
import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.juul.stropping.InjectableInstance
import com.juul.stropping.Multibindings
import com.juul.stropping.ValueProvisioner
import com.juul.stropping.common.forceSet
import com.juul.stropping.createStringTag
import com.juul.stropping.javaTypeOf
import dagger.android.DaggerApplication
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.multibindings.IntoMap
import io.mockk.every
import io.mockk.mockk
import org.kodein.di.Kodein
import org.kodein.di.android.x.androidXModule
import org.kodein.di.conf.ConfigurableKodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import java.lang.reflect.Type
import javax.inject.Named
import kotlin.reflect.KClass

/**
 * Static api for replacing a [DaggerApplication]'s injector during runtime.
 *
 * @see ReplacementHandle
 */
object Replacements {
    private val application: Application by lazy {
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
    }

    private val componentClassToReplacementHandle = mutableMapOf<KClass<*>, ReplacementHandle>()

    /** Gets and configures a [ReplacementHandle] for the component of type [T]. */
    inline fun <reified T : Any> of(
        resetConfiguration: Boolean = true,
        noinline configureReplacements: ReplacementHandle.() -> Unit
    ) = of(
        T::class,
        resetConfiguration,
        configureReplacements
    )

    /** Gets and configures a [ReplacementHandle] for the component of type [T]. */
    fun <T : Any> of(
        componentClass: KClass<T>,
        resetConfiguration: Boolean = true,
        configureReplacements: ReplacementHandle.() -> Unit
    ) {
        val handle = componentClassToReplacementHandle.getOrPut(componentClass) {
            ReplacementHandle(application, componentClass.java)
        }
        if (resetConfiguration) {
            handle.reset()
        }
        configureReplacements(handle)
    }
}

/**
 * A wrapper around the replacement [DispatchingAndroidInjector]. If you have an instance of this
 * class, you are using it instead of the original Dagger injector.
 */
class ReplacementHandle(
    application: Application,
    componentClass: Class<*>
) {
    /** Proxy dispatching android injector. */
    private val proxyInjector = mockk<DispatchingAndroidInjector<Any>> {
        every { inject(any()) } answers { call ->
            val receiver = checkNotNull(call.invocation.args.single())
            kodein.inject(InjectableInstance(receiver))
        }
    }

    /** Mutable kodein graph. */
    private val kodein = ConfigurableKodein(mutable = true)

    /** Source application graph, created through reflection on the component class. */
    private val applicationGraph = Kodein(allowSilentOverride = true) {
        import(androidXModule(application))
        importDaggerComponent(kodein, componentClass)
        bind<DispatchingAndroidInjector<*>>() with instance(proxyInjector)
        bind<Context>() with instance(application)
        bind<Application>() with instance(application)
        Bind(createTypeToken(application::class.java)) with instance(application)
    }

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

    /** Undoes calls to [overwrite] and [addIntoMap] for future injections. */
    fun reset() {
        kodein.clear()
        kodein.addExtend(applicationGraph)
    }

    /**
     * Replaces the provision method in Dagger for type [T] with either no qualifiers, or a [Named]
     * qualifier, with one that provides the given [value].
     */
    inline fun <reified T : Any> overwrite(
        value: T,
        named: String? = null
    ) {
        // Don't use `forceGet` even though it would be perfect, because visibility.
        val kodeinField = this::class.java.getDeclaredField("kodein")
        kodeinField.isAccessible = true
        val kodein = kodeinField.get(this) as ConfigurableKodein
        kodeinField.isAccessible = false
        kodein.addConfig {
            val tag = createStringTag(listOfNotNull(named))
            bind<T>(overrides = true, tag = tag) with instance(value)
        }
    }

    /**
     * Replaces the provision method in Dagger for type [V] with either no qualifiers, or a [Named]
     * qualifier, with one the provides the given [value] and is annotated with [IntoMap]. The map
     * key is faked as one with a return type of [K] and a value of [key].
     */
    inline fun <reified K : Any, reified V : Any> addIntoMap(
        key: K,
        value: V,
        named: String? = null
    ) = addIntoMap(
        javaTypeOf<K>(), key,
        javaTypeOf<V>(), value, named
    )


    /** Implementation detail of `addIntoMap<K, V>`. You likely don't want to call this directly. */
    fun addIntoMap(
        keyType: Type,
        key: Any,
        valueType: Type,
        value: Any,
        named: String?
    ) {
        kodein.addConfig {
            val provisioner = ValueProvisioner(
                value = value,
                returnType = valueType,
                named = named,
                multibindings = Multibindings.ToMap(keyType, key),
                isSingleton = true
            )
            bind(kodein, provisioner)
        }
    }
}

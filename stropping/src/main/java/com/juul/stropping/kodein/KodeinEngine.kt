package com.juul.stropping.kodein

import android.app.Application
import android.content.Context
import android.util.Log
import com.juul.stropping.Engine
import com.juul.stropping.Graph
import com.juul.stropping.InjectableConstructor
import com.juul.stropping.Multibindings
import com.juul.stropping.QualifiedType
import com.juul.stropping.ValueProvisioner
import com.juul.stropping.forceGet
import com.juul.stropping.hasAnnotation
import dagger.android.DispatchingAndroidInjector
import org.kodein.di.Kodein
import org.kodein.di.TypeToken
import org.kodein.di.android.x.androidXModule
import org.kodein.di.bindings.NoArgKodeinBinding
import org.kodein.di.conf.ConfigurableKodein
import org.kodein.di.direct
import org.kodein.di.fullDispString
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.provider
import org.kodein.di.generic.singleton
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import javax.inject.Singleton
import kotlin.reflect.KClass

private const val TAG = "Stropping.Kodein"

class KodeinEngine(
    context: Application,
    graph: Graph,
    androidInjector: DispatchingAndroidInjector<*>
) : Engine {

    class Builder : Engine.Builder {
        override fun build(
            context: Application,
            graph: Graph,
            androidInjector: DispatchingAndroidInjector<*>
        ): Engine = KodeinEngine(context, graph, androidInjector)
    }

    /** Mutable kodein graph. */
    private val kodein = ConfigurableKodein(mutable = true).apply {
        val source = Kodein(allowSilentOverride = true) {
            import(androidXModule(context))
            bindFromDagger(this@KodeinEngine, graph)
            bind<DispatchingAndroidInjector<*>>() with instance(androidInjector)
            bind<Context>() with instance(context)
            bind<Application>() with instance(context)
            Bind(createTypeToken(context::class.java)) with instance(context)
        }
        addExtend(source, allowOverride = true)
    }

    /** TODO: Make this less of a god function */
    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> getInstanceOf(typeToken: TypeToken<T>, tag: Any?): T {
        val type = typeToken::class.java.getDeclaredField("trueType")
            .forceGet<Type>(typeToken)
        val typeName = type.fullDispString()
        Log.d(TAG, "Attemping to get instance of $typeName (tag=$tag)")
        if (type is ParameterizedType) {
            val raw = type.rawType
            if (raw == dagger.Lazy::class.java || raw == javax.inject.Provider::class.java) {
                Log.d(TAG, "Found dagger.Lazy or javax.inject.Provider -- deferring injection")
                val paramType = type.actualTypeArguments.first()
                return object : dagger.Lazy<Any>, javax.inject.Provider<Any> {
                    override fun get(): Any {
                        Log.d(TAG, "Lazy/Provider get() for $typeName (tag=$tag)")
                        return getInstanceOf(createTypeToken(paramType), tag)
                    }
                } as T
            }
            // TODO: Handle other param types.
        }
        val instance = try {
            kodein.direct.Instance(typeToken, tag)
        } catch (e: Kodein.NotFoundException) {
            Log.d(TAG, "Failed to inject $typeName (tag=$tag). Attempting automatic bind.")
            if (type is ParameterizedType) {
                throw UnsupportedOperationException(
                    "Cannot automatically bind parameterized type.",
                    e
                )
            }
            check(type is Class<*>)
            kodein.addConfig {
                val isSingleton = type.hasAnnotation<Singleton>()
                val constructor = checkNotNull(InjectableConstructor.fromClass(type))
                val binding = when (isSingleton) {
                    true -> singleton { inject(constructor) }
                    false -> provider { inject(constructor) }
                } as NoArgKodeinBinding<Any, T>
                Bind(typeToken, tag) with binding
            }
            kodein.direct.Instance(typeToken, tag)
        }
        Log.d(TAG, "Got $instance")
        return instance
    }

    override fun <T : Any> overwrite(type: Type, value: T, named: String?) {
        val provisioner = ValueProvisioner(value, type, named, Multibindings.Single)
        kodein.addConfig { bindProvisioner(this@KodeinEngine, provisioner) }
    }

    override fun <K : Any, V : Any> addIntoMap(
        keyType: Type,
        key: K,
        valueType: Type,
        value: V,
        named: String?
    ) {
        val multibindings = Multibindings.ToMap(keyType, key)
        val provisioner = ValueProvisioner(value, valueType, named, multibindings)
        kodein.addConfig { bindProvisioner(this@KodeinEngine, provisioner) }
    }

    override fun <T : Any> getInstanceOf(qualifiedType: QualifiedType): T {
        @Suppress("UNCHECKED_CAST")
        val typeToken = createTypeToken(qualifiedType.type) as TypeToken<T>
        return getInstanceOf(typeToken, qualifiedType.qualifierString())
    }
}

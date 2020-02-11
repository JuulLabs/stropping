package com.juul.stropping.kodein

import com.juul.stropping.BindsMethodProvisioner
import com.juul.stropping.Engine
import com.juul.stropping.Graph
import com.juul.stropping.InjectableConstructor
import com.juul.stropping.InjectableMethod
import com.juul.stropping.InjectableStaticMethod
import com.juul.stropping.Multibindings
import com.juul.stropping.ProvidesMethodProvisioner
import com.juul.stropping.Provisioner
import com.juul.stropping.ValueProvisioner
import com.juul.stropping.parameterize
import org.kodein.di.AnyToken
import org.kodein.di.Kodein
import org.kodein.di.TypeToken
import org.kodein.di.bindings.InSet
import org.kodein.di.bindings.SetBinding
import org.kodein.di.generic.provider
import org.kodein.di.generic.singleton
import org.kodein.di.jvmType
import javax.inject.Provider

internal fun Kodein.Builder.bindFromDagger(
    engine: Engine,
    componentClass: Class<*>
) {
    val graph = Graph(componentClass)
    for (provisioner in graph.provisioners) {
        bindProvisioner(engine, provisioner)
    }
}

internal fun Kodein.Builder.bindProvisioner(
    engine: Engine,
    provisioner: Provisioner
) {
    fun provide() = when (provisioner) {
        is BindsMethodProvisioner -> {
            @Suppress("UNCHECKED_CAST")
            val implementationClazz = provisioner.parameters.single().type as Class<Any>
            val constructor = InjectableConstructor.fromClass(implementationClazz)
            engine.inject(checkNotNull(constructor))
        }
        is ProvidesMethodProvisioner -> {
            if (provisioner.isStatic) {
                engine.inject(InjectableStaticMethod(provisioner.method))
            } else {
                val moduleConstructor = InjectableConstructor.fromClass(provisioner.declaringClass)
                val module = engine.inject(checkNotNull(moduleConstructor))
                engine.inject(InjectableMethod(module, provisioner.method))
            }
        }
        is ValueProvisioner -> provisioner.value
    }

    val typeToken = createTypeToken(provisioner.returnType)
    val tag = provisioner.qualifiedType.qualifierString()
    val isSingleton = provisioner.isSingleton
    @Suppress("UNCHECKED_CAST")
    when (val entry = provisioner.multibindings) {
        is Multibindings.Single -> bindSingle(typeToken, tag, isSingleton) { provide() }
        is Multibindings.ToSet -> bindIntoSet(typeToken, tag, isSingleton) { provide() }
        is Multibindings.ToMap -> bindIntoMap(typeToken, tag, isSingleton, entry) { provide() }
    }
}

private fun Kodein.Builder.bindSingle(
    typeToken: TypeToken<Any>,
    tag: String?,
    isSingleton: Boolean,
    provide: () -> Any
) {
    val binding = when (isSingleton) {
        true -> singleton { provide() }
        false -> provider { provide() }
    }
    try {
        Bind(typeToken, tag) with binding
    } catch (e: Kodein.OverridingException) {
        Bind(typeToken, tag, overrides = true) with binding
    }
}

private fun Kodein.Builder.bindIntoSet(
    typeToken: TypeToken<Any>,
    tag: String?,
    isSingleton: Boolean,
    provide: () -> Any
) {
    val binding = when (isSingleton) {
        true -> singleton { provide() }
        false -> provider { provide() }
    }
    val setType = Set::class.java.parameterize(typeToken.jvmType)
    @Suppress("UNCHECKED_CAST")
    val setTypeToken = createTypeToken(setType) as TypeToken<Set<Any>>
    try {
        Bind(typeToken, tag).InSet(setTypeToken) with binding
    } catch (e: Kodein.OverridingException) {
        Bind(typeToken, tag, overrides = true).InSet(setTypeToken) with binding
    } catch (e: IllegalStateException) {
        // Create set
        Bind(tag) from SetBinding(AnyToken, typeToken, setTypeToken)
        // Retry after the setup
        bindIntoSet(typeToken, tag, isSingleton, provide)
    }
}

private fun Kodein.Builder.bindIntoMap(
    typeToken: TypeToken<Any>,
    tag: String?,
    isSingleton: Boolean,
    toMap: Multibindings.ToMap,
    provide: () -> Any
) {
    val binding = when (isSingleton) {
        true -> singleton { toMap.keyValue to provide() }
        false -> provider { toMap.keyValue to provide() }
    }

    // Types used to bind into the set of pairs
    val pairType = Pair::class.java.parameterize(toMap.keyType, typeToken.jvmType)
    val setType = Set::class.java.parameterize(pairType)
    val pairTypeToken = createTypeToken(pairType)
    @Suppress("UNCHECKED_CAST")
    val setTypeToken = createTypeToken(setType) as TypeToken<Set<Any>>

    try {
        Bind(pairTypeToken, tag).InSet(setTypeToken) with binding
    } catch (e: Kodein.OverridingException) {
        Bind(pairTypeToken, tag, overrides = true).InSet(setTypeToken) with binding
    }  catch (e: IllegalStateException) {
        // Types used to provide maps
        val directMapType = Map::class.java.parameterize(toMap.keyType, typeToken.jvmType)
        val providerType = Provider::class.java.parameterize(typeToken.jvmType)
        val indirectMapType = Map::class.java.parameterize(toMap.keyType, providerType)
        val mapTypeToken = createTypeToken(directMapType)
        val indirectMapTypeToken = createTypeToken(indirectMapType)

        // Create set & maps
        Bind(tag) from SetBinding(AnyToken, pairTypeToken, setTypeToken)
        Bind(mapTypeToken, tag) with provider {
            @Suppress("UNCHECKED_CAST")
            (Instance(setTypeToken, tag) as Set<Pair<*, *>>).toMap()
        }
        Bind(indirectMapTypeToken, tag) with provider {
            (Instance(mapTypeToken, tag) as Map<*, *>).mapValues { (_, v) ->
                Provider { v }
            }
        }

        // Retry after the setup
        bindIntoMap(typeToken, tag, isSingleton, toMap, provide)
    }
}

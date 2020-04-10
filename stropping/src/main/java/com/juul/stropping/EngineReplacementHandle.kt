package com.juul.stropping

import android.app.Application
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import java.lang.reflect.Proxy
import java.lang.reflect.Type
import javax.inject.Provider
import kotlin.reflect.KClass

class EngineReplacementHandle(
    application: Application,
    componentClass: KClass<*>,
    engineBuilder: Engine.Builder
) : ReplacementHandle() {

    private val graph: Graph = Graph(componentClass.java)

    private val engine: Engine by lazy {
        engineBuilder.build(application, graph, androidInjector)
    }

    override val component: Any by lazy {
        Proxy.newProxyInstance(
            application.classLoader,
            arrayOf(componentClass.java)
        ) { proxy, method, args ->
            if (method.name == "inject") {
                // TODO: Check if there is a more robust way to detect this.
                engine.inject(InjectableInstance(args[0]))
            } else {
                val qualifiedType = Provisioner.fromMethod(method)?.qualifiedType
                checkNotNull(qualifiedType) {
                    "Failed to call ${method.name} in ${componentClass.java.canonicalName} - invalid provision."
                }
                engine.getInstanceOf(qualifiedType)
            }
        }
    }

    override val androidInjector: DispatchingAndroidInjector<*> by lazy {
        val injector = AndroidInjector<Any> { engine.inject(InjectableInstance(it)) }
        val factory = AndroidInjector.Factory<Any> { injector }
        val provider = Provider<AndroidInjector.Factory<*>> { factory }
        val classInjectMap = graph.androidInjectable.associateWith { provider }
        val stringInjectMap = emptyMap<String, Provider<AndroidInjector.Factory<*>>>()
        DispatchingAndroidInjector::class.java.declaredConstructors.single()
            .apply { isAccessible = true }
            .newInstance(classInjectMap, stringInjectMap) as DispatchingAndroidInjector<*>
    }

    override fun <T : Any> overwrite(type: Type, value: T, named: String?) =
        engine.overwrite(type, value, named)

    override fun <T : Any> provide(type: Type, named: String?): T =
        engine.getInstanceOf(QualifiedType(type, listOfNotNull(named)))

    override fun <K : Any, V : Any> addIntoMap(
        keyType: Type,
        key: K,
        valueType: Type,
        value: V,
        named: String?
    ) = engine.addIntoMap(keyType, key, valueType, value, named)
}

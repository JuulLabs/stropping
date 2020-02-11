package com.juul.stropping

import android.app.Application
import dagger.android.DispatchingAndroidInjector
import io.mockk.every
import io.mockk.mockk
import java.lang.reflect.Type
import kotlin.reflect.KClass

class EngineReplacementHandle(
    application: Application,
    componentClass: KClass<*>,
    engineBuilder: Engine.Builder
) : ReplacementHandle() {

    private val engine: Engine by lazy {
        engineBuilder.build(application, componentClass, androidInjector)
    }

    override val androidInjector: DispatchingAndroidInjector<*> =
        mockk<DispatchingAndroidInjector<Any>> {
            every { inject(any()) } answers { call ->
                val receiver = checkNotNull(call.invocation.args.single())
                engine.inject(InjectableInstance(receiver))
            }
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

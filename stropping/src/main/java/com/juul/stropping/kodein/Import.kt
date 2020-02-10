package com.juul.stropping.kodein

import com.juul.stropping.Graph
import org.kodein.di.Kodein
import org.kodein.di.conf.ConfigurableKodein

internal fun Kodein.Builder.importDaggerComponent(
    configurable: ConfigurableKodein,
    componentClass: Class<*>
) {
    val graph = Graph(componentClass)
    for (function in graph.provisioners) {
        bind(configurable, function)
    }
}

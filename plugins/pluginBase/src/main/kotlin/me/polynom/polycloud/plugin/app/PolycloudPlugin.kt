package me.polynom.polycloud.plugin

/**
 * Interface that each PolyCloud plugin should implement.
 */
interface PolycloudPlugin {
    /**
     * Function that is run to register the plugin against Polycloud.
     */
    fun register()
}

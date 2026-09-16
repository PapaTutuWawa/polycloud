package me.polynom.polycloud.plugin.auth.dto

/**
 * Identifying data describing an auth plugin.
 */
data class AuthPluginData(
    /** The name of the plugin. */
    val pluginName: String,
    /** The scheme inside the Authorization header that this plugin handles. If null, then it's not registered. */
    val scheme: String?,
    /** The display name of the auth method. */
    val displayName: String,
    /** Icon to show in a UI. Optional. */
    val iconUrl: String?,
    /** Extra data to include in the endpoint response. */
    val data: Map<String, String>?,
)

package me.polynom.polycloud.auth

/**
 * Collection of constants related to authentication.
 */
object AuthenticationData {
    /** The header to use for authentication. */
    const val HEADER = "Authorization"

    /** Key in the request properties that contains the plugin auth response. */
    const val REQUEST_ATTRIBUTE_RESULT = "authorization"

    /** Key in the request properties that indicates that the client tried to authenticate. */
    const val REQUEST_ATTRIBUTE_PRESENT = "authorization-present"
}

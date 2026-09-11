package me.polynom.polycloud.apps.auth.oidc.jwt

import com.auth0.jwk.JwkProvider
import com.auth0.jwt.interfaces.RSAKeyProvider
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

class RSAKeyProvider(private val provider: JwkProvider) : RSAKeyProvider{
    override fun getPublicKeyById(keyId: String): RSAPublicKey {
        return provider.get(keyId).publicKey as RSAPublicKey
    }

    override fun getPrivateKey(): RSAPrivateKey? {
        TODO("Not yet implemented")
    }

    override fun getPrivateKeyId(): String? {
        TODO("Not yet implemented")
    }
}
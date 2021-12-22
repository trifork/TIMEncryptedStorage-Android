package com.trifork.timencryptedstorage.shared

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

object SecretKeyHelper {
    private const val blockMode: String = KeyProperties.BLOCK_MODE_CBC
    private const val padding: String = KeyProperties.ENCRYPTION_PADDING_PKCS7
    private const val algorithm: String = KeyProperties.KEY_ALGORITHM_AES

    private const val keyProvider = "AndroidKeyStore"
    private const val keyName = "SECRET_KEY_NAME"

    fun getOrCreateSecretKey(): SecretKey {
        //If a secret key exist return it
        getSecretKey()?.let { return it }

        //No secret key exist, create one
        return generateSecretKey(
            KeyGenParameterSpec.Builder(keyName, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(blockMode)
                .setEncryptionPaddings(padding)
                .setUserAuthenticationRequired(true)
                .build()
        )
    }

    private fun generateSecretKey(keyGenParameterSpec: KeyGenParameterSpec): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            algorithm, keyProvider
        )
        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    private fun getSecretKey(): SecretKey? {
        val keyStore = KeyStore.getInstance(keyProvider)

        // Before the keystore can be accessed, it must be loaded.
        keyStore.load(null)
        return keyStore.getKey(keyName, null)?.let { return it as SecretKey }
    }
}
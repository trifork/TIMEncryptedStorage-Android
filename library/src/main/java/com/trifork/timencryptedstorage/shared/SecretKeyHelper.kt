package com.trifork.timencryptedstorage.shared

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

//TODO Do we need to vary the keyName in case the user has two apps with the TIMSDK?
object SecretKeyHelper {
    private const val keyProvider = "AndroidKeyStore"
    private const val keyName = "TIM_SECRET_KEY"

    fun getOrCreateSecretKey(): SecretKey {
        //If a secret key exist return it. We want to use the same SecretKey across encrypt and decrypt
        getSecretKey()?.let { return it }

        //No secret key exist, create one
        return generateSecretKey(
            KeyGenParameterSpec.Builder(keyName, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(CipherConstants.cipherBlockMode)
                .setEncryptionPaddings(CipherConstants.cipherPadding)
                .setKeySize(CipherConstants.tagLengthInBits)
                .build()
        )
    }

    private fun generateSecretKey(keyGenParameterSpec: KeyGenParameterSpec): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            CipherConstants.cipherAlgorithm, keyProvider
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
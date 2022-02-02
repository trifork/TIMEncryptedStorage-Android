package com.trifork.timencryptedstorage.shared

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import com.trifork.timencryptedstorage.models.TIMResult
import com.trifork.timencryptedstorage.models.errors.TIMEncryptedStorageError
import com.trifork.timencryptedstorage.models.toTIMFailure
import com.trifork.timencryptedstorage.models.toTIMSuccess
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

object SecretKeyHelper {
    private const val keyProvider = "AndroidKeyStore"
    private const val keyName = "TIM_SECRET_KEY"

    // This is the official way of implementing generation of secret keys
    // BE AWARE If we need to change anything regarding the KeyGenParameterSpec, we would need to wipe existing SecretKeys stored on devices
    // following the current implementation we get the secret key already generated, with the old KeyGenParameterSpec, therefore not getting the updated Spec.
    fun getOrCreateSecretKey(): TIMResult<SecretKey, TIMEncryptedStorageError> {
        return try {
            // If a secret key exist return it. We want to use the same SecretKey across encrypt and decrypt
            getSecretKey()?.let { return it.toTIMSuccess() }

            // No secret key exist, create one
            generateSecretKey(
                KeyGenParameterSpec.Builder(keyName, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(CipherConstants.cipherBlockMode)
                    .setEncryptionPaddings(CipherConstants.cipherPadding)
                    .setKeySize(CipherConstants.tagLengthInBits)
                    .setUserAuthenticationRequired(true)
                    .setInvalidatedByBiometricEnrollment(true)
                    .build()
            ).toTIMSuccess()
        } catch (throwable: Throwable) {
            TIMEncryptedStorageError.InvalidEncryptionKey(throwable).toTIMFailure()
        }
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
package com.trifork.timencryptedstorage.shared

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.trifork.timencryptedstorage.models.TIMResult
import com.trifork.timencryptedstorage.models.errors.TIMEncryptedStorageError
import com.trifork.timencryptedstorage.models.toTIMFailure
import com.trifork.timencryptedstorage.models.toTIMSuccess
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

object SecretKeyHelper {
    private const val keyProvider = "AndroidKeyStore"

    // This is the official way of implementing generation of secret keys
    // BE AWARE If we need to change anything regarding the KeyGenParameterSpec, we would need to wipe existing SecretKeys stored on devices
    // following the current implementation we get the secret key already generated, with the old KeyGenParameterSpec, therefore not getting the updated Spec.
    //TODO Should we introduce a KeyGenParameterSpec version flag, so that a 'minimum_required' flag could be checked and used to invalidate the current secretKey
    fun getOrCreateSecretKey(keyId: String): TIMResult<SecretKey, TIMEncryptedStorageError> {
        return try {
            val existingKey = getSecretKey(keyId)
            if (existingKey != null) {
                return existingKey.toTIMSuccess()
            }
            generateSecretKey(createKeyGenParameterSpecForNewKeys(keyId)).toTIMSuccess()
        } catch (throwable: Throwable) {
            TIMEncryptedStorageError.InvalidEncryptionKey(throwable).toTIMFailure()
        }
    }

    fun createNewSecretKey(keyId: String): TIMResult<SecretKey, TIMEncryptedStorageError> {
        deleteSecretKey(getKeyAlias(keyId))
        return getOrCreateSecretKey(keyId)
    }

    fun deleteSecretKey(keyId: String) {
        val keyStore = loadKeyStore()
        keyStore.deleteEntry(getKeyAlias(keyId))
    }

    //region private helpers

    private fun getKeyAlias(keyId: String) = "TIMEncryptedStorage.secretKey.$keyId"


    private fun createKeyGenParameterSpecForNewKeys(keyId: String): KeyGenParameterSpec {
        return KeyGenParameterSpec.Builder(getKeyAlias(keyId), KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(BiometricCipherConstants.cipherBlockMode)
            .setEncryptionPaddings(BiometricCipherConstants.cipherPadding)
            .setUserAuthenticationValidityDurationSeconds(6 * 10)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
            .build()
    }

    private fun generateSecretKey(keyGenParameterSpec: KeyGenParameterSpec): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            BiometricCipherConstants.cipherAlgorithm, keyProvider
        )
        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    private fun getSecretKey(keyId: String): SecretKey? {
        val keyStore = loadKeyStore()
        return keyStore.getKey(getKeyAlias(keyId), null)?.let { it as SecretKey }
    }

    private fun loadKeyStore(): KeyStore {
        val keyStore = KeyStore.getInstance(keyProvider)
        // Before the keystore can be accessed, it must be loaded.
        keyStore.load(null)
        return keyStore
    }

    //endregion
}
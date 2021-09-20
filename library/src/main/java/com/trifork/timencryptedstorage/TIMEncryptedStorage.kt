package com.trifork.timencryptedstorage

import android.content.Context
import com.trifork.timencryptedstorage.keyservice.TIMKeyService
import com.trifork.timencryptedstorage.keyservice.TIMKeyServiceImpl
import com.trifork.timencryptedstorage.models.TIMESEncryptionMethod
import com.trifork.timencryptedstorage.models.TIMResult
import com.trifork.timencryptedstorage.models.errors.TIMEncryptedStorageError
import com.trifork.timencryptedstorage.models.errors.TIMKeyServiceError
import com.trifork.timencryptedstorage.models.keyservice.TIMESKeyCreationResult
import com.trifork.timencryptedstorage.models.keyservice.response.TIMKeyModel
import com.trifork.timencryptedstorage.securestorage.TIMEncryptedSharedPreferences
import com.trifork.timencryptedstorage.securestorage.TIMSecureStorage
import com.trifork.timencryptedstorage.shared.extensions.decrypt
import com.trifork.timencryptedstorage.shared.extensions.encrypt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

typealias StorageKey = String

/**
 * Trifork Identity Manager Encrypted Storage class
 *
 * The purpose of this classes is to store and load encrypted data based on a secret or biometric protection.
 *
 * This class depends on the Trifork Identity Manager Key Service ([TIMKeyService]), which handles encryption keys.
 */
class TIMEncryptedStorage(
    private val secureStorage: TIMSecureStorage,
    private val keyService: TIMKeyService,
    private val encryptionMethod: TIMESEncryptionMethod
) {

    //region Contains checks
    /**
     * Checks whether there is a stored value in the secure storage.
     * @param storageKey: The key for the stored item.
     * @return True if the item is present, otherwise false
     */
    fun hasValue(storageKey: StorageKey): Boolean = secureStorage.hasValue(storageKey)

    /**
     * Checks whether there is a stored value in the secure storage with biometric protection.
     * @param storageKey The key for the stored item
     * @param keyId The identifier for the key that was used when it was saved.
     * @return True of the item is present, otherwise false
     */
    fun hasBiometricProtectedValue(storageKey: StorageKey, keyId: String): Boolean = TODO()
    //endregion

    //region Removal
    /**
     * Removes the entry for the specified [storageKey]
     * @param storageKey The key for the stored item
     */
    fun remove(storageKey: StorageKey): Unit = secureStorage.remove(storageKey)

    /**
     * Removes the longSecret from secure storage
     * This vil disable biometric protection for all values with the specified [keyId]
     * @param keyId The key id for the entry to remove to remove
     */
    fun removeLongSecret(keyId: String): Unit = secureStorage.remove(getLongSecretStorageKey(keyId))
    //endregion

    private fun getLongSecretStorageKey(keyId: String) = "TIMEncryptedStorage.longSecret.$keyId"

    //region Storing
    /**
     * Stores and encrypts [data] for a [keyId] and [secret] combination
     * @param storageKey The key for the data, used to access the data again
     * @param data The data to encrypt and store
     * @param keyId The identifier for the key that was created with the secret
     * @param secret The secret for the key
     * @return [TIMResult] with an empty success result or an error
     */
    fun store(
        scope: CoroutineScope,
        storageKey: StorageKey,
        data: ByteArray,
        keyId: String,
        secret: String
    ): Deferred<TIMResult<Unit, TIMEncryptedStorageError>> = scope.async {
        // 1. Get encryption key with keyId + secret
        // 2. Encrypt data with encryption key from response
        // 3. Store encrypted data in secure storage with id
        // 4. Return bool result for success
        val keyServiceResult =
            keyService.getKeyViaSecret(scope, secret, keyId).await()
        return@async handleKeyServiceResultAndEncryptData(storageKey, data, keyServiceResult)
    }


    //region Handle results, encryption/decryption and storing
    private fun handleKeyServiceResultAndEncryptData(
        storageKey: StorageKey,
        data: ByteArray,
        keyServiceResult: TIMResult<TIMKeyModel, TIMKeyServiceError>
    ): TIMResult<Unit, TIMEncryptedStorageError> {
        return when (keyServiceResult) {
            is TIMResult.Failure -> TODO("Errors not handled")
            is TIMResult.Success -> encryptAndStore(storageKey, data, keyServiceResult.value)
        }
    }

    private fun encryptAndStore(
        storageKey: StorageKey,
        data: ByteArray,
        keyModel: TIMKeyModel
    ): TIMResult<Unit, TIMEncryptedStorageError> {
        val encryptedData = keyModel.encrypt(data, encryptionMethod)
        // TODO: Can we ever fail to write to EncryptedSharedPrefs? - MFJ (13/09/2021)
        return TIMResult.Success(secureStorage.store(encryptedData, storageKey))
    }

    private fun handleKeyServiceResultAndDecryptData(
        storageKey: StorageKey,
        keyServiceResult: TIMResult<TIMKeyModel, TIMKeyServiceError>
    ): TIMResult<ByteArray, TIMEncryptedStorageError> {
        return when(keyServiceResult) {
            is TIMResult.Failure -> TODO("Errors not handled")
            is TIMResult.Success -> loadAndDecrypt(storageKey, keyServiceResult.value)
        }
    }

    private fun loadAndDecrypt(storageKey: StorageKey, keyModel: TIMKeyModel): TIMResult<ByteArray, TIMEncryptedStorageError> {
        val encryptedDataResult = secureStorage.get(storageKey)

        return when(encryptedDataResult) {
            is TIMResult.Failure -> TODO()
            // TODO: Consider error cases here - MFJ (13/09/2021)
            is TIMResult.Success ->
                TIMResult.Success(keyModel.decrypt(encryptedDataResult.value, encryptionMethod))
        }
    }
    //endregion

    /**
     * Stores and encrypts [data] for a [keyId] and [longSecret] combination
     * @param storageKey The key for the data, used to access the data again
     * @param data The data to encrypt and store
     * @param keyId The identifier for the key that was created with the secret
     * @param longSecret The secret for the key
     * @return [TIMResult] with an empty success result or an error
     */
    fun storeWithLongSecret(
        scope: CoroutineScope,
        storageKey: StorageKey,
        data: ByteArray,
        keyId: String,
        longSecret: String
    ): Deferred<TIMResult<Unit, TIMEncryptedStorageError>> = scope.async {
        // 1. Get encryption key with keyId + longSecret
        // 2. Encrypt data with encryption key from response
        // 3. Store encrypted data in secure storage with id
        // 4. Return bool result for success
        val keyServiceResult =
            keyService.getKeyViaLongSecret(scope, longSecret, keyId).await()
        return@async handleKeyServiceResultAndEncryptData(storageKey, data, keyServiceResult)
    }


    /**
     * Creates a new encryption key with the [secret] and encrypts and stores the [data]
     * @param storageKey The key for the data, used to access the data again
     * @param data The data to encrypt and store
     * @param secret The secret for the new key
     */
    fun storeWithNewKey(
        scope: CoroutineScope,
        storageKey: StorageKey,
        data: ByteArray,
        secret: String
    ): Deferred<TIMResult.Success<TIMESKeyCreationResult>> = scope.async {
        // 1. Create new encryption key with secret
        // 2. Encrypt data with encryption key from response
        // 3. Store encrypted data in secure storage with id
        // 4. Return bool result for success + keyId
        val keyServiceResult = keyService.createKey(scope, secret).await()
        when (keyServiceResult) {
            is TIMResult.Failure -> TODO()
            is TIMResult.Success -> {
                val keyModel = keyServiceResult.value
                // TODO: Ask Peter if any instances are still running where longSecret is nullable - MFJ (13/09/2021)
                val encryptedData = keyModel.encrypt(data, encryptionMethod)

                // TODO: Consider error cases here - MFJ (13/09/2021)
                TIMResult.Success(TIMESKeyCreationResult(keyModel.keyId, keyModel.longSecret))
            }
        }
    }

    fun storeViaBiometric(
        storageKey: StorageKey,
        data: ByteArray,
        keyId: String
    ): Nothing {
        // 1. Load longSecret for keyId via FaceID/TouchID
        // 2. Call store(id: id, data: data, keyId: keyId, longSecret: <loadedLongSecret>)
        // 3. Return result of store function
        TODO()
    }


    fun storeViaBiometricWithNewKey(): Nothing {
        // 1. Create new encryption key with secret
        // 2. Save longSecret for keyId via FaceID/TouchID
        // 3. Encrypt data with encryption key from response
        // 4. Store encrypted data in secure storage with id
        // 5. Return bool result for success + keyId
        TODO()
    }
    //endregion

    //region Getters
    /**
     * Gets and decrypts data for a [keyId] and [secret] combination
     */
    fun get(
        scope: CoroutineScope,
        storageKey: StorageKey,
        keyId: String,
        secret: String
    ): Deferred<TIMResult<ByteArray, TIMEncryptedStorageError>> = scope.async {
        // 1. Get encryption key with keyId + secret
        // 2. Load encrypted data from secure storage with id
        // 3. Decrypt data with encryption key
        // 4. Return decrypted data
        val keyServiceResult = keyService.getKeyViaSecret(
            scope,
            secret,
            keyId
        ).await()

        return@async handleKeyServiceResultAndDecryptData(storageKey, keyServiceResult)
    }

    fun getViaBiomtric(): Nothing = TODO()

    fun enableBiometric(): TIMResult<Unit, TIMEncryptedStorageError> = TODO()
    //endregion

}
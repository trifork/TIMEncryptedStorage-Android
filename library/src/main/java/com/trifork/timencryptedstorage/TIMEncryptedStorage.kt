package com.trifork.timencryptedstorage

import android.util.Log.DEBUG
import com.trifork.timencryptedstorage.helpers.TIMEncryptedStorageLogger
import com.trifork.timencryptedstorage.keyservice.TIMKeyService
import com.trifork.timencryptedstorage.models.*
import com.trifork.timencryptedstorage.models.errors.TIMEncryptedStorageError
import com.trifork.timencryptedstorage.models.errors.TIMKeyServiceError
import com.trifork.timencryptedstorage.models.keyservice.TIMESKeyCreationResult
import com.trifork.timencryptedstorage.models.keyservice.response.TIMKeyModel
import com.trifork.timencryptedstorage.securestorage.TIMSecureStorage
import com.trifork.timencryptedstorage.shared.BiometricCipherHelper
import com.trifork.timencryptedstorage.shared.BiometricEncryptedDataHelper

import com.trifork.timencryptedstorage.shared.extensions.asPreservedByteArray
import com.trifork.timencryptedstorage.shared.extensions.asPreservedString
import com.trifork.timencryptedstorage.shared.extensions.decrypt
import com.trifork.timencryptedstorage.shared.extensions.encrypt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import javax.crypto.Cipher

typealias StorageKey = String

/**
 * Trifork Identity Manager Encrypted Storage class
 *
 * The purpose of this classes is to store and load encrypted data based on a secret or biometric protection.
 *
 * This class depends on the Trifork Identity Manager Key Service ([TIMKeyService]), which handles encryption keys.
 */

class TIMEncryptedStorage(
    val secureStorage: TIMSecureStorage,
    private val logger: TIMEncryptedStorageLogger,
    private val keyService: TIMKeyService,
    private val encryptionMethod: TIMESEncryptionMethod
) {

    private val biometricCipherHelper = BiometricCipherHelper(logger)

    companion object {
        private const val TAG = "TIMEncryptedStorage"
    }

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
    fun hasBiometricProtectedValue(storageKey: StorageKey, keyId: String): Boolean {
        val secureStoreKey = longSecretSecureStoreId(keyId)
        return secureStorage.hasBiometricProtectedValue(secureStoreKey) && secureStorage.hasValue(storageKey)
    }
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
    fun removeLongSecret(keyId: String) {
        remove(getLongSecretStorageKey(keyId))
        biometricCipherHelper.deleteSecretKey(keyId)
    }
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
            is TIMResult.Failure -> TIMEncryptedStorageError.KeyServiceFailed(keyServiceResult.error).toTIMFailure()
            is TIMResult.Success -> encryptAndStore(storageKey, data, keyServiceResult.value)
        }
    }

    private fun encryptAndStore(
        storageKey: StorageKey,
        data: ByteArray,
        keyModel: TIMKeyModel
    ): TIMResult<Unit, TIMEncryptedStorageError> {
        val encryptedData = keyModel.encrypt(data, encryptionMethod)

        return when (encryptedData) {
            is TIMResult.Failure -> encryptedData
            is TIMResult.Success -> {
                secureStorage.store(encryptedData.value, storageKey).toTIMSuccess()
            }
        }
    }

    private fun handleKeyServiceResultAndDecryptData(
        storageKey: StorageKey,
        keyServiceResult: TIMResult<TIMKeyModel, TIMKeyServiceError>
    ): TIMResult<ByteArray, TIMEncryptedStorageError> {
        return when (keyServiceResult) {
            is TIMResult.Failure -> TIMEncryptedStorageError.KeyServiceFailed(keyServiceResult.error).toTIMFailure()
            is TIMResult.Success -> loadAndDecrypt(storageKey, keyServiceResult.value)
        }
    }

    /**
     * Gets [StorageKey] from SecureStorage and decrypts using [keyModel] [decrypt] helper method
     * @param storageKey The key for the data, used when the data was saved
     * @param keyModel The [TIMKeyModel] received from the key server
     */
    private fun loadAndDecrypt(storageKey: StorageKey, keyModel: TIMKeyModel): TIMResult<ByteArray, TIMEncryptedStorageError> {
        val encryptedDataResult = secureStorage.get(storageKey)

        return when (encryptedDataResult) {
            is TIMResult.Failure -> TIMEncryptedStorageError.SecureStorageFailed(encryptedDataResult.error).toTIMFailure()
            is TIMResult.Success -> keyModel.decrypt(encryptedDataResult.value, encryptionMethod)
        }
    }
    //endregion

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
    ): Deferred<TIMResult<TIMESKeyCreationResult, TIMEncryptedStorageError>> = scope.async {
        // 1. Create new encryption key with secret
        // 2. Encrypt data with encryption key from response
        // 3. Store encrypted data in secure storage with id
        // 4. Return bool result for success + keyId
        val keyServiceResult = keyService.createKey(scope, secret).await()
        val keyModel = when (keyServiceResult) {
            is TIMResult.Failure -> return@async TIMEncryptedStorageError.KeyServiceFailed(keyServiceResult.error).toTIMFailure()
            is TIMResult.Success -> keyServiceResult.value
        }

        val encryptAndStoreResult = encryptAndStore(storageKey, data, keyServiceResult.value)

        if (encryptAndStoreResult is TIMResult.Failure) {
            return@async TIMEncryptedStorageError.FailedToEncryptData(encryptAndStoreResult.error).toTIMFailure()
        }

        TIMESKeyCreationResult(keyModel.keyId, keyModel.longSecret).toTIMSuccess()
    }

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
        val keyServiceResult = keyService.getKeyViaLongSecret(scope, longSecret, keyId).await()
        return@async handleKeyServiceResultAndEncryptData(storageKey, data, keyServiceResult)
    }

    //TODO This function is not utilized in our final implementation and therefore only called by test code (as in the iOS implementation) JHE (09.03.22)
    //TODO Should we also store the long secret? using storeLongSecret(storageKey, longSecret, cipher) JHE (28/12/21)
    fun storeViaBiometric(
        scope: CoroutineScope,
        storageKey: StorageKey,
        data: ByteArray,
        keyId: String
    ): Deferred<TIMResult<Unit, TIMEncryptedStorageError>> = scope.async {
        // 1. Load longSecret for keyId via FaceID/TouchID
        // 2. Call store(id: id, data: data, keyId: keyId, longSecret: <loadedLongSecret>)
        // 3. Return result of store function
        val secureStorageKey = longSecretSecureStoreId(keyId)
        val longSecretResult = secureStorage.getBiometricProtected(secureStorageKey)

        val longSecret = when (longSecretResult) {
            is TIMResult.Failure -> return@async TIMEncryptedStorageError.SecureStorageFailed(longSecretResult.error).toTIMFailure()
            is TIMResult.Success -> longSecretResult.value.asPreservedString
        }

        return@async storeWithLongSecret(
            scope,
            storageKey,
            data,
            keyId,
            longSecret
        ).await()
    }

    //TODO This function is not utilized in our final implementation and therefore only called by test code (as in the iOS implementation) JHE (09.03.22)
    fun storeViaBiometricWithNewKey(scope: CoroutineScope, id: StorageKey, data: ByteArray, secret: String, cipher: Cipher): Deferred<TIMResult<TIMESKeyCreationResult, TIMEncryptedStorageError>> = scope.async {
        // 1. Create new encryption key with secret
        // 2. Save longSecret for keyId via FaceID/TouchID
        // 3. Encrypt data with encryption key from response
        // 4. Store encrypted data in secure storage with id
        // 5. Return bool result for success + keyId
        val createKeyResult = keyService.createKey(scope, secret).await()

        when (createKeyResult) {
            is TIMResult.Failure -> TIMEncryptedStorageError.KeyServiceFailed(createKeyResult.error).toTIMFailure()
            is TIMResult.Success -> {
                val keyModel = createKeyResult.value
                val longSecret = keyModel.longSecret

                val storeLongSecretResult = storeLongSecret(keyModel.keyId, longSecret, cipher)

                if (storeLongSecretResult is TIMResult.Failure) {
                    return@async storeLongSecretResult.error.toTIMFailure()
                }

                val encryptResult = encryptAndStore(id, data, keyModel)

                return@async when (encryptResult) {
                    is TIMResult.Failure -> encryptResult.error.toTIMFailure()
                    is TIMResult.Success -> TIMESKeyCreationResult(keyModel.keyId, keyModel.longSecret).toTIMSuccess()
                }
            }
        }

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

    fun getViaBiometric(scope: CoroutineScope, id: StorageKey, keyId: String, cipher: Cipher): Deferred<TIMResult<TIMESBiometricLoadResult, TIMEncryptedStorageError>> = scope.async {
        // 1. Load longSecret for keyId via FaceID/TouchID
        // 2. Get encryption key with keyId + longSecret
        // 3. Load encrypted data from secure storage with id
        // 4. Decrypt data with encryption key
        // 5. Return decrypted data + longSecret

        val getBiometricEncryptedDataResult = getBiometricEncryptedAndDecryptData(cipher, keyId)

        val longSecret = when (getBiometricEncryptedDataResult) {
            is TIMResult.Failure -> return@async getBiometricEncryptedDataResult.error.toTIMFailure()
            is TIMResult.Success -> getBiometricEncryptedDataResult.value
        }

        logger.log(DEBUG, TAG, "Got and decrypted the longSecret")

        val getKeyViaLongSecretResult = keyService.getKeyViaLongSecret(scope, longSecret, keyId).await()

        val timKeyModelResult = when (getKeyViaLongSecretResult) {
            is TIMResult.Failure -> return@async TIMEncryptedStorageError.UnexpectedData().toTIMFailure()
            is TIMResult.Success -> handleKeyServiceResultAndDecryptData(id, getKeyViaLongSecretResult)
        }

        logger.log(DEBUG, TAG, "Got the TIMKeyModel from the key server and decrypted the data using the key model")

        return@async when (timKeyModelResult) {
            is TIMResult.Failure -> timKeyModelResult.error.toTIMFailure()
            is TIMResult.Success -> TIMESBiometricLoadResult(timKeyModelResult.value, longSecret).toTIMSuccess()
        }
    }

    private fun getBiometricEncryptedAndDecryptData(cipher: Cipher, keyId: String): TIMResult<String, TIMEncryptedStorageError> {
        val biometricProtectedResult = getBiometricEncryptedData(longSecretSecureStoreId(keyId))

        val biometricEncryptedData = when (biometricProtectedResult) {
            is TIMResult.Failure -> return biometricProtectedResult
            is TIMResult.Success -> biometricProtectedResult.value
        }

        logger.log(DEBUG, TAG, "Got biometric encrypted data from secure storage")

        val decryptedDataResult = biometricCipherHelper.decrypt(cipher, biometricEncryptedData.encryptedData)

        return when (decryptedDataResult) {
            is TIMResult.Failure -> decryptedDataResult
            is TIMResult.Success -> decryptedDataResult.value.asPreservedString.toTIMSuccess()
        }
    }

    /**
     * Get BiometricEncryptedData object that was stored using [storeLongSecret]
     * @param [keyId] the keyId used to save the data
     * @return [BiometricEncryptedData] object where the initialization vector (IV) and biometricEncryptedData can be found
     */
    private fun getBiometricEncryptedData(keyId: String): TIMResult<BiometricEncryptedDataHelper.BiometricEncryptedData, TIMEncryptedStorageError> {
        val storeResult = secureStorage.getBiometricProtected(keyId)

        val encryptedData = when (storeResult) {
            is TIMResult.Failure -> return TIMEncryptedStorageError.SecureStorageFailed(storeResult.error).toTIMFailure()
            is TIMResult.Success -> storeResult.value
        }

        logger.log(DEBUG, TAG, "Got biometric encrypted data from secure storage")

        return BiometricEncryptedDataHelper.biometricEncryptedData(encryptedData)
    }

    /**
     * Enables biometric protection for a keyId, by getting the `longSecret` and saving it with biometric protection.
     * @param scope the [CoroutineScope]
     * @param keyId the identifier for the key that was created with the `secret`.
     * @param secret the secret which was used to create the `keyId`
     * @param cipher the cipher received from the biometric prompt
     */
    fun enableBiometric(
        scope: CoroutineScope,
        keyId: String,
        secret: String,
        cipher: Cipher
    ): Deferred<TIMResult<Unit, TIMEncryptedStorageError>> = scope.async {
        // 1. Get longSecret with keyId + secret
        // 2. Save longSecret for keyId encrypted by Cipher from biometric login
        // 3. Return bool result for success
        val keyServiceResult = keyService.getKeyViaSecret(scope, secret, keyId).await()

        val keyModel = when (keyServiceResult) {
            is TIMResult.Failure -> return@async TIMEncryptedStorageError.KeyServiceFailed(keyServiceResult.error).toTIMFailure()
            is TIMResult.Success -> keyServiceResult.value
        }

        logger.log(DEBUG, TAG, "Got keyModel for storing long secret")

        storeLongSecret(keyModel.keyId, keyModel.longSecret, cipher)
    }


    /**
     * Enables biometric protection for a keyId, by saving the `longSecret` with biometric protection.
     * @param keyId The identifier for the key that was created with the `longSecret`.
     * @param longSecret The longSecret which was created with the `keyId`.
     * @param cipher the cipher which should be used to encrypt the `longSecret`.
     */
    fun enableBiometric(
        keyId: String,
        longSecret: String,
        cipher: Cipher
    ): TIMResult<Unit, TIMEncryptedStorageError> = storeLongSecret(keyId, longSecret, cipher)

    //endregion

    //region Biometric helper methods

    fun getEncryptCipher(keyId: String): TIMResult<Cipher, TIMEncryptedStorageError> {
        return biometricCipherHelper.getInitializedCipherForEncryption(keyId)
    }

    /**
     * Gets the decryption cipher that is sent to biometric prompt. The keyId is used to get the initialization vector (IV) from the previously used encryption cipher.
     * @param [keyId] the keyId for the data that should be decrypted
     * @return TIMResult with either [Cipher] or [TIMEncryptedStorageError]
     */
    fun getDecryptCipher(keyId: String): TIMResult<Cipher, TIMEncryptedStorageError> {
        //Get the previously saved BiometricEncryptedData to retrieve the IV from the encryption cipher, which we need when creating the encryption cipher
        val biometricEncryptedDataResult = getBiometricEncryptedData(longSecretSecureStoreId(keyId))

        val initializationVector = when (biometricEncryptedDataResult) {
            is TIMResult.Failure -> return biometricEncryptedDataResult
            is TIMResult.Success -> biometricEncryptedDataResult.value.initializationVector
        }

        logger.log(DEBUG, TAG, "Got initialization vector (IV) for initializing decryption cipher")
        //Initialize the decryption cipher using the retrieved IV, can now be send to the biometric prompt
        return biometricCipherHelper.getInitializedCipherForDecryption(keyId, initializationVector)
    }

    /**
     * Encrypt and store [longSecret] with the cipher initialization vector (IV) using a [BiometricEncryptedDataHelper.BiometricEncryptedData] object
     * @param keyId the keyId which will be the key in the key value pair store (the key the data will be saved with). The original keyId should be parsed to [longSecretSecureStoreId] to get the correct store id.
     * @param longSecret the longSecret received from the key server.
     * @param cipher the cipher received from the biometric prompt. Used to encrypt the longSecret, the initialization vector (IV) is saved for decryption usage.
     */
    private fun storeLongSecret(keyId: String, longSecret: String, cipher: Cipher): TIMResult<Unit, TIMEncryptedStorageError> {
        //Encrypt data using cipher
        val encryptedDataResult = biometricCipherHelper.encrypt(cipher, longSecret.asPreservedByteArray)

        val encryptedData = when (encryptedDataResult) {
            is TIMResult.Failure -> return encryptedDataResult
            is TIMResult.Success -> encryptedDataResult.value
        }

        logger.log(DEBUG, TAG, "Encrypted longSecret using cipher")

        //Create a BiometricEncryptedData json string, for storing
        val biometricEncryptedDataResult = BiometricEncryptedDataHelper.biometricEncryptedDataJSON(
            encryptedData,
            cipher.iv
        )

        val biometricEncryptedData = when (biometricEncryptedDataResult) {
            is TIMResult.Failure -> return biometricEncryptedDataResult
            is TIMResult.Success -> biometricEncryptedDataResult.value
        }

        logger.log(DEBUG, TAG, "Encoded longSecret for storing")

        //Store the BiometricEncryptedData using our secureStorage
        //TODO Should we use our encryptAndStore function instead, so the iv is also encrypted when storing? JHE (05.01.22)
        val storeResult = secureStorage.storeBiometricProtected(biometricEncryptedData, longSecretSecureStoreId(keyId))

        return when (storeResult) {
            is TIMResult.Failure -> TIMEncryptedStorageError.SecureStorageFailed(storeResult.error).toTIMFailure()
            is TIMResult.Success -> Unit.toTIMSuccess()
        }
    }

    private fun longSecretSecureStoreId(keyId: String) = "TIMEncryptedStorage.longSecret.$keyId"

    //endregion
}
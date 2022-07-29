package com.trifork.timencryptedstorage.shared

import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Log
import com.trifork.timencryptedstorage.helpers.TIMEncryptedStorageLogger
import com.trifork.timencryptedstorage.models.*
import com.trifork.timencryptedstorage.models.errors.TIMEncryptedStorageError
import com.trifork.timencryptedstorage.shared.SecretKeyHelper.deleteSecretKey
import com.trifork.timencryptedstorage.shared.extensions.asPreservedByteArray
import com.trifork.timencryptedstorage.shared.extensions.asPreservedString
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

/**
 * BiometricEncryptedDataHelper is a util object that can create a bytearray for storing biometric encrypted data.
 * The biometric encrypted data and its initialization vector (IV) is encoded into a json string before being turned into a bytearray for storing.
 * The IV is necessary when creating the decryption cipher and is therefore stored with the encrypted data.
 */
object BiometricEncryptedDataHelper {

    @Serializable
    class BiometricEncryptedData(
        val encryptedData: ByteArray,
        val initializationVector: ByteArray
    )

    /**
     * Converts biometric encrypted data and its initialization vector (IV) into a serializable [BiometricEncryptedData] object, serializes it and preserves it as a bytearray
     * @param data The biometric encrypted data
     * @param initializationVector The initialization vector from the biometric cipher
     * @return A bytearray representation of the created [BiometricEncryptedData] object
     */
    fun biometricEncryptedDataJSON(data: ByteArray, initializationVector: ByteArray): TIMResult<ByteArray, TIMEncryptedStorageError> {
        val biometricEncryptedData = BiometricEncryptedData(
            data,
            initializationVector
        )

        return try {
            Json.encodeToString(biometricEncryptedData).asPreservedByteArray.toTIMSuccess()
        } catch (throwable: Throwable) {
            TIMEncryptedStorageError.FailedToEncodeData(throwable).toTIMFailure()
        }
    }

    /**
     * Converts the stored bytearray containing the encrypted data and its initialization vector (IV) into a serializable [BiometricEncryptedData] object
     * @param data The stored bytearray
     * @return A [BiometricEncryptedData] object containing the biometric encrypted data and the (IV) from the encryption cipher
     */
    fun biometricEncryptedData(data: ByteArray): TIMResult<BiometricEncryptedData, TIMEncryptedStorageError> {
        return try {
            Json.decodeFromString<BiometricEncryptedData>(data.asPreservedString).toTIMSuccess()
        } catch (throwable: Throwable) {
            TIMEncryptedStorageError.FailedToDecodeData(throwable).toTIMFailure()
        }
    }
}

object BiometricCipherConstants {
    const val cipherAlgorithm: String = KeyProperties.KEY_ALGORITHM_AES
    const val cipherBlockMode: String = KeyProperties.BLOCK_MODE_CBC
    const val cipherPadding: String = KeyProperties.ENCRYPTION_PADDING_PKCS7

    const val cipherTransformation = "${cipherAlgorithm}/${cipherBlockMode}/${cipherPadding}"
}

class BiometricCipherHelper(private val logger: TIMEncryptedStorageLogger) {

    companion object {
        private const val TAG = "BiometricCipherHelper"
    }

    //region Encrypt and decrypt
    fun encrypt(cipher: Cipher, data: ByteArray): TIMResult<ByteArray, TIMEncryptedStorageError> {
        return try {
            cipher.doFinal(data).toTIMSuccess()
        } catch (throwable: IllegalBlockSizeException) {
            //Filter so we only catch the android.security.KeyStoreException child exception
            val childThrowable = throwable.cause
            if (childThrowable != null && childThrowable.message?.contains("Key user not authenticated") == true) {
                logger.log(Log.DEBUG, TAG, "encrypt threw: IllegalBlockSizeException with KeyStoreException as cause exception", throwable)
                return TIMEncryptedStorageError.UnrecoverablyFailedToEncrypt(throwable).toTIMFailure()
            }
            logger.log(Log.DEBUG, TAG, "encrypt threw: IllegalBlockSizeException", throwable)
            TIMEncryptedStorageError.FailedToEncryptData(throwable).toTIMFailure()
        } catch (throwable: Throwable) {
            logger.log(Log.DEBUG, TAG, "encrypt threw", throwable)
            TIMEncryptedStorageError.FailedToEncryptData(throwable).toTIMFailure()
        }
    }

    fun decrypt(cipher: Cipher, data: ByteArray): TIMResult<ByteArray, TIMEncryptedStorageError> {
        return try {
            cipher.doFinal(data).toTIMSuccess()
        } catch (throwable: IllegalBlockSizeException) {
            //Filter so we only catch the android.security.KeyStoreException child exception
            val childThrowable = throwable.cause
            if (childThrowable != null && childThrowable.message?.contains("Key user not authenticated") == true) {
                logger.log(Log.DEBUG, TAG, "decrypt threw: IllegalBlockSizeException with KeyStoreException as cause exception", throwable)
                return TIMEncryptedStorageError.UnrecoverablyFailedToDecrypt(throwable).toTIMFailure()
            }
            logger.log(Log.DEBUG, TAG, "decrypt threw: IllegalBlockSizeException", throwable)
            TIMEncryptedStorageError.FailedToEncryptData(throwable).toTIMFailure()
        } catch (throwable: Throwable) {
            logger.log(Log.DEBUG, TAG, "encrypt decrypt", throwable)
            TIMEncryptedStorageError.FailedToDecryptData(throwable).toTIMFailure()
        }
    }
    //endregion

    //region Initialization of encryption and decryption ciphers
    fun getInitializedCipherForEncryption(keyId: String): TIMResult<Cipher, TIMEncryptedStorageError> {
        val cipherResult = getCipherInstance()

        logger.log(Log.DEBUG, TAG, "getInitializedCipherForEncryption: cipher result: $cipherResult")

        val cipher = cipherResult.mapValueOrOnFailedResult {
            return@getInitializedCipherForEncryption it
        }

        val secretKeyResult = SecretKeyHelper.createNewSecretKey(keyId)

        logger.log(Log.DEBUG, TAG, "getInitializedCipherForEncryption: secretKeyResult: $secretKeyResult")

        val secretKey = secretKeyResult.mapValueOrOnFailedResult {
            return@getInitializedCipherForEncryption it
        }

        return initializeCipher(
            initialize = {
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                cipher.toTIMSuccess()
            },
            onKeyPermanentlyInvalidated = {
                logger.log(Log.DEBUG, TAG, "Deleting secret key and calling getInitializedCipherForEncryption again.")
                //This case can happen when a user with a invalid key tries to enable biometric after the key was invalidated
                deleteSecretKey(keyId)
                getInitializedCipherForEncryption(keyId)
            }
        )
    }

    fun getInitializedCipherForDecryption(keyId: String, initializationVector: ByteArray): TIMResult<Cipher, TIMEncryptedStorageError> {
        //Create a cipher and gets the secret key for the parsed keyId
        val cipherSecretKeyResult = createCipherAndSecretKey(keyId)

        logger.log(Log.DEBUG, TAG, "getInitializedCipherForDecryption: cipherSecretKeyResult: $cipherSecretKeyResult")

        val cipherSecretKey = cipherSecretKeyResult.mapValueOrOnFailedResult {
            return@getInitializedCipherForDecryption it
        }

        return initializeCipher(
            initialize = {
                cipherSecretKey.cipher.init(Cipher.DECRYPT_MODE, cipherSecretKey.secretKey, IvParameterSpec(initializationVector))
                cipherSecretKey.cipher.toTIMSuccess()
            },
            onKeyPermanentlyInvalidated = { throwable ->
                logger.log(Log.DEBUG, TAG, "Deleting secret key and throwing TIMEncryptedStorageError.PermanentlyInvalidatedKey")
                deleteSecretKey(keyId)
                TIMEncryptedStorageError.PermanentlyInvalidatedKey(throwable).toTIMFailure()
            }
        )
    }

    private fun initializeCipher(
        initialize: () -> TIMResult<Cipher, TIMEncryptedStorageError>,
        onKeyPermanentlyInvalidated: (throwable: Throwable) -> TIMResult<Cipher, TIMEncryptedStorageError>,
    ): TIMResult<Cipher, TIMEncryptedStorageError> {
        return try {
            initialize()
        } catch (throwable: KeyPermanentlyInvalidatedException) {
            logger.log(Log.DEBUG, TAG, "", throwable)
            onKeyPermanentlyInvalidated()
        } catch (throwable: Throwable) {
            logger.log(Log.DEBUG, TAG, "", throwable)
            TIMEncryptedStorageError.InvalidEncryptionKey(throwable).toTIMFailure()
        }
    }

    fun deleteSecretKey(keyId: String) {
        SecretKeyHelper.deleteSecretKey(keyId)
    }
    //endregion

    //region private helpers
    /**
     * Holder of cipher and secret key
     */
    internal class CipherSecretKey(val cipher: Cipher, val secretKey: SecretKey)

    /**
     * Gets the cipher instance and gets/creates the secret key for the parsed keyId
     * [keyId] The keyId the secret key should be get/created for
     * @return A [TIMResult] with a [CipherSecretKey] containing the created cipher and secret key or a [TIMEncryptedStorageError]
     */
    private fun createCipherAndSecretKey(keyId: String): TIMResult<CipherSecretKey, TIMEncryptedStorageError> {
        val cipherResult = getCipherInstance()

        logger.log(Log.DEBUG, TAG, "createCipherAndSecretKey: cipherResult: $cipherResult")

        val cipher = cipherResult.mapValueOrOnFailedResult {
            return@createCipherAndSecretKey it
        }

        val secretKeyResult = SecretKeyHelper.getOrCreateSecretKey(keyId)

        logger.log(Log.DEBUG, TAG, "createCipherAndSecretKey: secretKeyResult: $secretKeyResult")

        val secretKey = secretKeyResult.mapValueOrOnFailedResult {
            return@createCipherAndSecretKey it
        }


        return CipherSecretKey(cipher, secretKey).toTIMSuccess()
    }

    /**
     * Uses [BiometricCipherConstants] to get a cipher instance with the defined [CipherConstants.cipherTransformation]
     * @return A [TIMResult] with a [Cipher] or a [TIMEncryptedStorageError.InvalidCipher]
     */
    private fun getCipherInstance(): TIMResult<Cipher, TIMEncryptedStorageError> {
        return try {
            Cipher.getInstance(BiometricCipherConstants.cipherTransformation).toTIMSuccess()
        } catch (throwable: Throwable) {
            logger.log(Log.DEBUG, TAG, "", throwable)
            TIMEncryptedStorageError.InvalidCipher(throwable).toTIMFailure()
        }
    }
    //endregion
}
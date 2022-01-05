package com.trifork.timencryptedstorage.shared

import com.trifork.timencryptedstorage.models.TIMResult
import com.trifork.timencryptedstorage.models.errors.TIMEncryptedStorageError
import com.trifork.timencryptedstorage.models.toTIMFailure
import com.trifork.timencryptedstorage.models.toTIMSuccess
import com.trifork.timencryptedstorage.shared.extensions.asPreservedByteArray
import com.trifork.timencryptedstorage.shared.extensions.asPreservedString
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

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
        }
        catch (throwable: Throwable) {
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

object BiometricCipherHelper {
    //region Encrypt and decrypt
    fun encrypt(cipher: Cipher, data: ByteArray): TIMResult<ByteArray, TIMEncryptedStorageError> {
        return try {
            cipher.doFinal(data).toTIMSuccess()
        } catch (throwable: Throwable) {
            TIMEncryptedStorageError.FailedToEncryptData(throwable).toTIMFailure()
        }
    }

    fun decrypt(cipher: Cipher, data: ByteArray): TIMResult<ByteArray, TIMEncryptedStorageError> {
        return try {
            cipher.doFinal(data).toTIMSuccess()
        } catch (throwable: Throwable) {
            TIMEncryptedStorageError.FailedToDecryptData(throwable).toTIMFailure()
        }
    }
    //endregion

    //region Initialization of encryption and decryption ciphers
    fun getInitializedCipherForEncryption(): TIMResult<Cipher, TIMEncryptedStorageError> {
        return handleCipherAndSecretKeyCreation { cipher, secretKey ->
            try {
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                cipher.toTIMSuccess()
            } catch (throwable: Throwable) {
                TIMEncryptedStorageError.InvalidEncryptionKey(throwable).toTIMFailure()
            }

        }
    }

    fun getInitializedCipherForDecryption(initializationVector: ByteArray): TIMResult<Cipher, TIMEncryptedStorageError> {
        return handleCipherAndSecretKeyCreation { cipher, secretKey ->
            try {
                cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(CipherConstants.tagLengthInBits, initializationVector))
                cipher.toTIMSuccess()
            } catch (throwable: Throwable) {
                TIMEncryptedStorageError.InvalidEncryptionKey(throwable).toTIMFailure()
            }

        }
    }
    //endregion

    //region private helpers
    private fun getCipherInstance(): TIMResult<Cipher, TIMEncryptedStorageError> {
        return try {
            Cipher.getInstance(CipherConstants.cipherTransformation).toTIMSuccess()
        } catch (throwable: Throwable) {
            TIMEncryptedStorageError.InvalidCipher(throwable).toTIMFailure()
        }

    }

    private fun handleCipherAndSecretKeyCreation(initCipher: (cipher: Cipher, secretKey: SecretKey) -> TIMResult<Cipher, TIMEncryptedStorageError>): TIMResult<Cipher, TIMEncryptedStorageError> {
        val cipherResult = getCipherInstance()

        val cipher = when (cipherResult) {
            is TIMResult.Failure -> return cipherResult
            is TIMResult.Success -> cipherResult.value
        }

        val secretKeyResult = SecretKeyHelper.getOrCreateSecretKey()

        val secretKey = when (secretKeyResult) {
            is TIMResult.Failure -> return secretKeyResult
            is TIMResult.Success -> secretKeyResult.value
        }

        return initCipher(cipher, secretKey)
    }
    //endregion
}
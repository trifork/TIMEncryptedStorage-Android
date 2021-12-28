package com.trifork.timencryptedstorage.shared

import com.trifork.timencryptedstorage.shared.extensions.asPreservedByteArray
import com.trifork.timencryptedstorage.shared.extensions.asPreservedString
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

object BiometricEncryptedDataHelper {

    @Serializable
    class BiometricEncryptedData(
        val encryptedData: ByteArray,
        val initializationVector: ByteArray
    )

    fun biometricEncryptedDataJSON(data: ByteArray, initializationVector: ByteArray): ByteArray {
        val biometricEncryptedData = BiometricEncryptedData(
            data,
            initializationVector
        )

        return Json.encodeToString(biometricEncryptedData).asPreservedByteArray
    }

    fun biometricEncryptedData(data: ByteArray): BiometricEncryptedData {
        //TODO Can actually fail if the stored string is in a different format, handle JsonDecodingException
        return Json.decodeFromString(data.asPreservedString)
    }
}

object BiometricCipherHelper {
    //TODO Missing error handling
    fun encrypt(cipher: Cipher, data: ByteArray): ByteArray = cipher.doFinal(data)

    //TODO Missing error handling
    fun decrypt(cipher: Cipher, data: ByteArray): ByteArray = cipher.doFinal(data)

    //TODO Missing error handling
    private fun getCipherInstance(): Cipher {
        return Cipher.getInstance(CipherConstants.cipherTransformation)
    }

    //TODO Missing error handling
    fun getInitializedCipherForEncryption(): Cipher {
        val cipher = getCipherInstance()
        val secretKey = SecretKeyHelper.getOrCreateSecretKey()
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher
    }

    //TODO Missing error handling
    fun getInitializedCipherForDecryption(initializationVector: ByteArray): Cipher {
        val cipher = getCipherInstance()
        val secretKey = SecretKeyHelper.getOrCreateSecretKey()
        cipher.init(
            Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(CipherConstants.tagLengthInBits, initializationVector)
        )
        return cipher
    }
}
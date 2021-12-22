package com.trifork.timencryptedstorage.shared

import android.security.keystore.KeyProperties
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

object BiometricCipherHelper {

    private const val aesAlgorithmName = "AES"
    private const val ivLengthInBytes = 12
    private const val tagLengthInBits = 128

    //TODO Missing error handling
    fun encrypt(cipher: Cipher, data: ByteArray): ByteArray = cipher.doFinal(data)

    //TODO Missing error handling
    fun decrypt(cipher: Cipher, data: ByteArray): ByteArray = cipher.doFinal(data)

    //TODO Missing error handling
    private fun getCipherInstance(): Cipher {
        return Cipher.getInstance(
            aesAlgorithmName + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7
        )
    }

    //TODO Missing error handling
    fun getInitializedCipherForEncryption(): Cipher {
        val cipher = getCipherInstance()
        val secretKey = SecretKeyHelper.getOrCreateSecretKey()
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher
    }

    //TODO Missing error handling
    fun getInitializedCipherForDecryption(): Cipher {
        val cipher = getCipherInstance()
        val secretKey = SecretKeyHelper.getOrCreateSecretKey()
        cipher.init(
            Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(tagLengthInBits, generateInitialisationVector())
        )
        return cipher
    }

    private fun generateInitialisationVector(): ByteArray {
        val iv = ByteArray(ivLengthInBytes)
        SecureRandom().nextBytes(iv)
        return iv
    }
}
package com.trifork.timencryptedstorage.shared

import android.security.keystore.KeyProperties
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.GCMParameterSpec

private const val aesAlgorithmName = "AES"

sealed class CipherHelper {

    internal val ivLengthInBytes = 12
    internal val tagLengthInBits = 128

    companion object {
        const val cipherTransformation = "$aesAlgorithmName/GCM/NoPadding"
    }

    @Throws(NoSuchAlgorithmException::class, NoSuchPaddingException::class)
    internal open fun getCipherInstance(): Cipher {
        return Cipher.getInstance(cipherTransformation)
    }

    @Throws(IllegalArgumentException::class)
    internal fun getGCMParamSpecFromData(data: ByteArray) = GCMParameterSpec(tagLengthInBits, data, 0, ivLengthInBytes)

    @Throws(IllegalArgumentException::class)
    internal fun getGCMParamSpec(iv: ByteArray): GCMParameterSpec = GCMParameterSpec(tagLengthInBits, iv)

    internal fun generateInitialisationVector(): ByteArray {
        val iv = ByteArray(ivLengthInBytes)
        SecureRandom().nextBytes(iv)
        return iv
    }

}

class BiometricCipherHelper : CipherHelper() {

    private val secretKeyHelper = SecretKeyHelper()

    //TODO Missing error handling
    fun encrypt(cipher: Cipher, data: ByteArray): ByteArray = cipher.doFinal(data)

    //TODO Missing error handling
    fun decrypt(cipher: Cipher, data: ByteArray): ByteArray = cipher.doFinal(data)

    //TODO Missing error handling
    //We cannot use the same cipher instance unfortunately
    override fun getCipherInstance(): Cipher {
        return Cipher.getInstance(
            aesAlgorithmName + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7
        )
    }

    //TODO Missing error handling
    fun getInitializedCipherForEncryption(): Cipher {
        val cipher = getCipherInstance()
        val secretKey = secretKeyHelper.getOrCreateSecretKey()
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher
    }

    //TODO Missing error handling
    fun getInitializedCipherForDecryption(): Cipher {
        val cipher = getCipherInstance()
        val secretKey = secretKeyHelper.getOrCreateSecretKey()
        cipher.init(
            Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(tagLengthInBits, generateInitialisationVector())
        )
        return cipher
    }
}
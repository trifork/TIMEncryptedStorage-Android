package com.trifork.timencryptedstorage.shared

import java.nio.ByteBuffer
import java.nio.ReadOnlyBufferException
import java.security.*
import javax.crypto.*
import javax.crypto.spec.GCMParameterSpec

object GCMCipherHelper {
    const val aesAlgorithmName = "AES"

    private const val ivLengthInBytes = 12
    private const val tagLengthInBits = 128
    private const val cipherAlgorithm = "${aesAlgorithmName}/GCM/NoPadding"

    @Throws(UnsupportedOperationException::class, InvalidKeyException::class, InvalidAlgorithmParameterException::class, IllegalStateException::class, IllegalBlockSizeException::class, BadPaddingException::class, AEADBadTagException::class)
    fun encrypt(key: Key, data: ByteArray): ByteArray {
        val iv = generateInitialisationVector()
        val parameterSpec = getGCMParamSpec(iv)

        val cipherText = with(getCipherInstance()) {
            init(Cipher.ENCRYPT_MODE, key, parameterSpec)
            doFinal(data)
        }
        return combine(iv, cipherText)
    }

    @Throws(UnsupportedOperationException::class, InvalidKeyException::class, InvalidAlgorithmParameterException::class, IllegalStateException::class, IllegalBlockSizeException::class, BadPaddingException::class, AEADBadTagException::class)
    fun decrypt(key: Key, data: ByteArray): ByteArray {
        val iv = getGCMParamSpecFromData(data)

        return with(getCipherInstance()) {
            init(Cipher.DECRYPT_MODE, key, iv)
            doFinal(data, ivLengthInBytes, data.size - ivLengthInBytes)
        }
    }

    @Throws(IllegalArgumentException::class, ReadOnlyBufferException::class, UnsupportedOperationException::class)
    private fun combine(iv: ByteArray, cipherText: ByteArray): ByteArray {
        return ByteBuffer.allocate(iv.size + cipherText.size).apply {
            put(iv)
            put(cipherText)
        }.array()
    }

    @Throws(NoSuchAlgorithmException::class, NoSuchPaddingException::class)
    private fun getCipherInstance(): Cipher = Cipher.getInstance(cipherAlgorithm)

    @Throws(IllegalArgumentException::class)
    private fun getGCMParamSpecFromData(data: ByteArray) =
        GCMParameterSpec(tagLengthInBits, data, 0, ivLengthInBytes)

    @Throws(IllegalArgumentException::class)
    private fun getGCMParamSpec(iv: ByteArray): GCMParameterSpec =
        GCMParameterSpec(tagLengthInBits, iv)

    private fun generateInitialisationVector(): ByteArray {
        val iv = ByteArray(ivLengthInBytes)
        SecureRandom().nextBytes(iv)
        return iv
    }
}
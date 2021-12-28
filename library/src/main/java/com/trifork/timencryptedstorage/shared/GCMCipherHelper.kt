package com.trifork.timencryptedstorage.shared

import android.security.keystore.KeyProperties
import com.trifork.timencryptedstorage.shared.CipherConstants.cipherTransformation
import com.trifork.timencryptedstorage.shared.CipherConstants.ivLengthInBytes
import com.trifork.timencryptedstorage.shared.CipherConstants.tagLengthInBits
import java.nio.ByteBuffer
import java.nio.ReadOnlyBufferException
import java.security.*
import javax.crypto.*
import javax.crypto.spec.GCMParameterSpec

object CipherConstants {
    const val cipherAlgorithm: String = KeyProperties.KEY_ALGORITHM_AES
    const val cipherBlockMode: String = KeyProperties.BLOCK_MODE_GCM
    const val cipherPadding: String = KeyProperties.ENCRYPTION_PADDING_NONE

    const val cipherTransformation = "${cipherAlgorithm}/${cipherBlockMode}/${cipherPadding}"

    const val ivLengthInBytes = 12
    const val tagLengthInBits = 128
}

object GCMCipherHelper {
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
    private fun getCipherInstance(): Cipher = Cipher.getInstance(cipherTransformation)

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
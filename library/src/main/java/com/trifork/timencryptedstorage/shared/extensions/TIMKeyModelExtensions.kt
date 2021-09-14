package com.trifork.timencryptedstorage.shared.extensions

import android.util.Base64
import com.trifork.timencryptedstorage.models.TIMESEncryptionMethod
import com.trifork.timencryptedstorage.models.TIMResult
import com.trifork.timencryptedstorage.models.errors.TIMEncryptedStorageError
import com.trifork.timencryptedstorage.models.keyservice.response.TIMKeyModel
import java.nio.ByteBuffer
import java.security.Key
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val aesAlgorithmName = "AES"

fun TIMKeyModel.encrypt(data: ByteArray, encryptionMethod: TIMESEncryptionMethod): ByteArray {
    val secretKeyResult = getAesKey()
    return when (secretKeyResult) {
        is TIMResult.Failure -> TODO()
        is TIMResult.Success -> try {
            when (encryptionMethod) {
                TIMESEncryptionMethod.AesGcm -> GCMCipherHelper.encrypt(
                    secretKeyResult.value,
                    data
                )
            }
        } catch (e: Throwable) {
            TODO("NO EXCEPTION HANDLING")
        }
    }
}

fun TIMKeyModel.decrypt(data: ByteArray, encryptionMethod: TIMESEncryptionMethod): ByteArray {
    val secretKeyResult = getAesKey()
    return when (secretKeyResult) {
        is TIMResult.Failure -> TODO()
        is TIMResult.Success -> when (encryptionMethod) {
            TIMESEncryptionMethod.AesGcm -> GCMCipherHelper.decrypt(
                secretKeyResult.value,
                data
            )
        }
    }
}

internal fun TIMKeyModel.getAesKey(): TIMResult<Key, TIMEncryptedStorageError> {
    return try {
        val decodedKey = Base64.decode(key, Base64.DEFAULT)
        val secretKey = SecretKeySpec(decodedKey, aesAlgorithmName)
        TIMResult.Success(secretKey)
    } catch (e: Throwable) {
        TODO("Both decoding and key creation can fail")
    }
}

private object GCMCipherHelper {
    private const val ivLengthInBytes = 12
    private const val tagLengthInBits = 128
    private const val cipherAlgorithm = "$aesAlgorithmName/GCM/NoPadding"

    fun encrypt(key: Key, data: ByteArray): ByteArray {
        val iv = generateInitialisationVector()
        val parameterSpec = getGCMParamSpec(iv)

        val cipherText = with(getCipherInstance()) {
            init(Cipher.ENCRYPT_MODE, key, parameterSpec)
            doFinal(data)
        }

        // TODO: NO EXCEPTION HANDLING - MFJ (14/09/2021)
        return combine(iv, cipherText)
    }

    fun decrypt(key: Key, data: ByteArray): ByteArray {
        val iv = getGCMParamSpecFromData(data)

        // TODO: NO EXCEPTION HANDLING - MFJ (14/09/2021)
        return with(getCipherInstance()) {
            init(Cipher.DECRYPT_MODE, key, iv)
            doFinal(data, ivLengthInBytes, data.size - ivLengthInBytes)
        }
    }

    private fun combine(iv: ByteArray, cipherText: ByteArray): ByteArray {
        // TODO: NO EXCEPTION HANDLING - MFJ (14/09/2021)
        return ByteBuffer.allocate(iv.size + cipherText.size).apply {
            put(iv)
            put(cipherText)
        }.array()
    }

    // TODO: NO EXCEPTION HANDLING - MFJ (14/09/2021)
    private fun getCipherInstance(): Cipher = Cipher.getInstance(cipherAlgorithm)

    // TODO: NO EXCEPTION HANDLING - MFJ (14/09/2021)
    private fun getGCMParamSpecFromData(data: ByteArray) =
        GCMParameterSpec(tagLengthInBits, data, 0, ivLengthInBytes)

    // TODO: NO EXCEPTION HANDLING - MFJ (14/09/2021)
    private fun getGCMParamSpec(iv: ByteArray): GCMParameterSpec =
        GCMParameterSpec(tagLengthInBits, iv)

    // TODO: NO EXCEPTION HANDLING - MFJ (14/09/2021)
    private fun generateInitialisationVector(): ByteArray {
        val iv = ByteArray(ivLengthInBytes)
        SecureRandom().nextBytes(iv)
        return iv
    }
}
package com.trifork.timencryptedstorage.shared.extensions

import android.util.Base64
import com.trifork.timencryptedstorage.models.TIMESEncryptionMethod
import com.trifork.timencryptedstorage.models.TIMResult
import com.trifork.timencryptedstorage.models.errors.TIMEncryptedStorageError
import com.trifork.timencryptedstorage.models.keyservice.response.TIMKeyModel
import com.trifork.timencryptedstorage.models.toTIMFailure
import com.trifork.timencryptedstorage.models.toTIMSuccess
import java.nio.ByteBuffer
import java.nio.ReadOnlyBufferException
import java.security.*
import javax.crypto.*
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val aesAlgorithmName = "AES"

fun TIMKeyModel.encrypt(data: ByteArray, encryptionMethod: TIMESEncryptionMethod): TIMResult<ByteArray, TIMEncryptedStorageError> {
    return try {
        val secretKeyResult = getAesKey()
        when (encryptionMethod) {
            TIMESEncryptionMethod.AesGcm -> GCMCipherHelper.encrypt(
                secretKeyResult,
                data
            ).toTIMSuccess()
        }
    } catch (e: Throwable) {
        TIMEncryptedStorageError.FailedToEncryptData(e).toTIMFailure()
    }
}


//Tag potentielt imod cipher?
fun TIMKeyModel.decrypt(data: ByteArray, encryptionMethod: TIMESEncryptionMethod): TIMResult<ByteArray, TIMEncryptedStorageError> {
    return try {
        val secretKeyResult = getAesKey()
        when (encryptionMethod) {
            TIMESEncryptionMethod.AesGcm -> GCMCipherHelper.decrypt(
                secretKeyResult,
                data
            ).toTIMSuccess()
        }
    } catch (e: Exception) {
        TIMEncryptedStorageError.FailedToDecryptData(e).toTIMFailure()
    }
}

@Throws(IllegalArgumentException::class, IllegalArgumentException::class)
internal fun TIMKeyModel.getAesKey(): Key {
    val decodedKey = Base64.decode(key, Base64.DEFAULT)
    return SecretKeySpec(decodedKey, aesAlgorithmName)
}

object GCMCipherHelper {
    private const val ivLengthInBytes = 12
    private const val tagLengthInBits = 128
    private const val cipherAlgorithm = "$aesAlgorithmName/GCM/NoPadding"

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
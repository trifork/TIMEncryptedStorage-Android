package com.trifork.timencryptedstorage.shared.extensions

import android.util.Base64
import com.trifork.timencryptedstorage.models.TIMESEncryptionMethod
import com.trifork.timencryptedstorage.models.TIMResult
import com.trifork.timencryptedstorage.models.errors.TIMEncryptedStorageError
import com.trifork.timencryptedstorage.models.keyservice.response.TIMKeyModel
import com.trifork.timencryptedstorage.models.toTIMFailure
import com.trifork.timencryptedstorage.models.toTIMSuccess
import com.trifork.timencryptedstorage.shared.CipherConstants
import com.trifork.timencryptedstorage.shared.GCMCipherHelper
import java.security.Key
import javax.crypto.spec.SecretKeySpec

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
    return SecretKeySpec(decodedKey, CipherConstants.cipherAlgorithm)
}
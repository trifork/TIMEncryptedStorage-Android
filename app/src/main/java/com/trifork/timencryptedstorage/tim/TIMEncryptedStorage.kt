package com.trifork.timencryptedstorage.tim

import com.trifork.timencryptedstorage.models.TIMESEncryptionMethod
import com.trifork.timencryptedstorage.tim.keyservice.TIMKeyService
import com.trifork.timencryptedstorage.tim.securestorage.TIMSecureStorage

typealias StorageId = String

class TIMEncryptedStorage(
    val secureStorage: TIMSecureStorage,
    val keyService: TIMKeyService,
    val encryptionMethod: TIMESEncryptionMethod
) {

    fun hasValue(id: StorageId): Boolean = secureStorage.has

    fun hasBiometricProtectedValue(id: StorageId, keyId: String): Boolean = TODO()

    fun remove(id: StorageId): Unit = TODO()

    fun removeLongSecret(keyId: String): Unit = TODO()

}
package com.trifork.timencryptedstorage.helpers.test

import com.trifork.timencryptedstorage.StorageKey
import com.trifork.timencryptedstorage.models.TIMResult
import com.trifork.timencryptedstorage.models.errors.TIMSecureStorageError
import com.trifork.timencryptedstorage.models.toTIMFailure
import com.trifork.timencryptedstorage.models.toTIMSuccess
import com.trifork.timencryptedstorage.securestorage.TIMSecureStorage
import com.trifork.timencryptedstorage.securestorage.TIMSecureStorageItem

class SecureStorageMockItem(id: String): TIMSecureStorageItem(id) {
    var isBioProtected : Boolean = false
}

class SecureStorageMock : TIMSecureStorage {
    private val bioProtectedData = HashMap<StorageKey, ByteArray>()
    private val protectedData = HashMap<StorageKey, ByteArray>()

    override fun remove(storageKey: StorageKey) {
        protectedData.remove(storageKey)
    }

    override fun store(data: ByteArray, storageItem: StorageKey) {
        protectedData.put(storageItem, data)
    }

    override fun get(storageKey: StorageKey): TIMResult<ByteArray, TIMSecureStorageError> {
        val data = protectedData[storageKey]
        if (data != null) {
            return data.toTIMSuccess()
        }
        return TIMSecureStorageError.FailedToLoadData(Throwable("No data found for key $storageKey")).toTIMFailure()
    }

    override fun hasValue(storageKey: StorageKey): Boolean = protectedData[storageKey] != null

    override fun storeBiometricProtected(data: ByteArray, storageKey: StorageKey): TIMResult<Unit, TIMSecureStorageError> {
        protectedData.put(storageKey, data)
        //storageKey.isBioProtected = true
        return Unit.toTIMSuccess()
    }
}
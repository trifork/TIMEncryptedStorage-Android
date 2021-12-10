package com.trifork.timencryptedstorage.helpers.test

import com.trifork.timencryptedstorage.StorageKey
import com.trifork.timencryptedstorage.models.TIMResult
import com.trifork.timencryptedstorage.models.errors.TIMSecureStorageError
import com.trifork.timencryptedstorage.models.toTIMFailure
import com.trifork.timencryptedstorage.models.toTIMSuccess
import com.trifork.timencryptedstorage.securestorage.TIMSecureStorage

class SecureStorageMock : TIMSecureStorage {

    private val protectedData = HashMap<StorageKey, ByteArray>()

    override fun remove(storageKey: StorageKey) {
        protectedData.remove(storageKey)
    }

    override fun store(data: ByteArray, storageKey: StorageKey) {
        protectedData.put(storageKey, data)
    }

    override fun get(storageKey: StorageKey): TIMResult<ByteArray, TIMSecureStorageError> {
        val data = protectedData.get(storageKey)
        if (data != null) {
            return data.toTIMSuccess()
        }
        return TIMSecureStorageError.FailedToLoadData(Throwable("No data found for key $storageKey")).toTIMFailure()
    }

    override fun hasValue(storageKey: StorageKey): Boolean = protectedData.get(storageKey) != null

}
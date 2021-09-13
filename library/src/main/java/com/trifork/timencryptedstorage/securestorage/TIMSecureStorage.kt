package com.trifork.timencryptedstorage.securestorage

import com.trifork.timencryptedstorage.StorageKey
import com.trifork.timencryptedstorage.models.TIMResult
import com.trifork.timencryptedstorage.models.errors.TIMSecureStorageError

/**
 * This protocol defines a secure store for data
 * The default implementation for TIM is [TIMEncryptedSharedPreferences]
 */
interface TIMSecureStorage {

    // TODO: No handling of biometric is currently implemented - MFJ (25/08/2021)

    /**
     * Removes the entry for [storageKey] from the secure storage
     * @param storageKey The id of the entry to remove
     */
    fun remove(storageKey: StorageKey)

    /**
     * Stores the [data] with the associated identifier [storageKey]
     * @param data The data to store
     * @param storageKey The key to identify the data with
     * @return An indication if the data was stored successfully
     */
    fun store(data: ByteArray, storageKey: StorageKey)

    /**
     * Gets data from the secure storage
     * @param storageKey The key to fetch data for
     * @return The data for the [storageKey] if it exists or an error describing what went wrong
     */
    fun get(storageKey: StorageKey): TIMResult<ByteArray, TIMSecureStorageError>

    /**
     * Checks whether data for a given [storageKey] exists
     * @param storageKey The key identifying the data
     * @return True if data for [storageKey] exists
     */
    fun hasValue(storageKey: StorageKey): Boolean
}
package com.trifork.timencryptedstorage.tim.securestorage

import com.trifork.timencryptedstorage.models.TIMResult
import com.trifork.timencryptedstorage.models.TIMSecureStorageError

/**
 * This protocol defines a secure store for data
 * The default implementation for TIM is [TIMEncryptedSharedPreferences]
 */
interface TIMSecureStorage {

    // TODO: No handling of biometric is currently implemented - MFJ (25/08/2021)

    /**
     * Removes the entry for [entryKey] from the secure storage
     * @param entryKey The id of the entry to remove
     */
    fun remove(entryKey: String)

    /**
     * Stores the [data] with the associated identifier [entryKey]
     * @param data The data to store
     * @param entryKey The key to identify the data with
     * @return An indication if the data was stored successfully
     */
    fun store(data: ByteArray, entryKey: String)

    /**
     * Gets data from the secure storage
     * @param entryKey The key to fetch data for
     * @return The data for the [entryKey] if it exists or an error describing what went wrong
     */
    fun get(entryKey: String): TIMResult<ByteArray, TIMSecureStorageError>

    /**
     * Checks whether data for a given [entryKey] exists
     * @param entryKey The key identifying the data
     * @return True if data for [entryKey] exists
     */
    fun hasValue(entryKey: String): Boolean
}
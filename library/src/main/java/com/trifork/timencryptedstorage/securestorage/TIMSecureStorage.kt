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


    /**
     * Saves data in the secure store with biometric protection (meaning that only Biometric login can unlock the access to the data)
     * @param data Data to save
     * @param storageKey The storageKey to identify the data
     */
    fun storeBiometricProtected(data: ByteArray, storageKey: StorageKey): TIMResult<Unit, TIMSecureStorageError>

    /**
     * Checks whether an item exists with biometric protection in the secure storage or not.
     * @param storageKey The storageKey that identifies the data (and which is was saved with)
     * @return True if the item exists, otherwise false
     */
    fun hasBiometricProtectedValue(storageKey: StorageKey): Boolean


    fun getBiometricProtected(storageKey: StorageKey): TIMResult<ByteArray, TIMSecureStorageError>

}
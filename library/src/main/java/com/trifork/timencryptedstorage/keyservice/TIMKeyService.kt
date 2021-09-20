package com.trifork.timencryptedstorage.keyservice

import com.trifork.timencryptedstorage.models.TIMResult
import com.trifork.timencryptedstorage.models.errors.TIMKeyServiceError
import com.trifork.timencryptedstorage.models.keyservice.response.TIMKeyModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred

/**
 * The key service responsible for creating and getting ecnryption keys based on a pin code or long secret (from biometric identification)_
 */
interface TIMKeyService {

    /**
     * Creates a new key
     * @param secret The secret to create the key with
     */
    fun createKey(scope: CoroutineScope, secret: String): Deferred<TIMResult<TIMKeyModel, TIMKeyServiceError>>

    /**
     * Gets an existing encryption key from the key service
     * @param secret The secret used when the key was created
     * @param keyId The identifier for the encryption key
     * @return The [TIMKeyModel] that is valid for the given [secret] and [keyId] combination, if any exists
     */
    fun getKeyViaSecret(
        scope: CoroutineScope,
        secret: String,
        keyId: String
    ): Deferred<TIMResult<TIMKeyModel, TIMKeyServiceError>>

    /**
     * Gets an existing encryption key from the key service
     * @param longSecret The [TIMKeyModel.longSecret] that was returned when the key was originally created via [TIMKeyService.createKey]
     * @param keyId The identifier for the encryption key
     * @return The [TIMKeyModel] that is valid for the given [longSecret] and [keyId] combination, if any exists
     */
    fun getKeyViaLongSecret(
        scope: CoroutineScope,
        longSecret: String,
        keyId: String
    ): Deferred<TIMResult<TIMKeyModel, TIMKeyServiceError>>

}
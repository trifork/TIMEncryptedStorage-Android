package com.trifork.timencryptedstorage.keyservice

import com.trifork.timencryptedstorage.models.TIMResult
import com.trifork.timencryptedstorage.models.errors.TIMKeyServiceError
import com.trifork.timencryptedstorage.models.keyservice.response.TIMKeyModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred

interface TIMKeyService {

    fun createKey(scope: CoroutineScope, secret: String): Deferred<TIMResult<TIMKeyModel, TIMKeyServiceError>>

    fun getKeyViaSecret(
        scope: CoroutineScope,
        secret: String,
        keyId: String
    ): Deferred<TIMResult<TIMKeyModel, TIMKeyServiceError>>

    fun getKeyViaLongSecret(
        scope: CoroutineScope,
        longSecret: String,
        keyId: String
    ): Deferred<TIMResult<TIMKeyModel, TIMKeyServiceError>>

}
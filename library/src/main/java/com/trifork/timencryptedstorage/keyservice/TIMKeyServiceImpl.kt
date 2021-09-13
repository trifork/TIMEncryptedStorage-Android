package com.trifork.timencryptedstorage.keyservice

import com.trifork.timencryptedstorage.models.TIMResult
import com.trifork.timencryptedstorage.models.errors.TIMKeyServiceError
import com.trifork.timencryptedstorage.models.keyservice.TIMKeyServiceConfiguration
import com.trifork.timencryptedstorage.models.keyservice.TIMKeyServiceVersion
import com.trifork.timencryptedstorage.models.keyservice.request.TIMKeyRequestBody
import com.trifork.timencryptedstorage.models.keyservice.response.TIMKeyModel
import com.trifork.timencryptedstorage.models.toTIMKeyServiceResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import retrofit2.Retrofit
import retrofit2.await

class TIMKeyServiceImpl private constructor(
    private val api: TIMKeyServiceAPI,
    private val version: TIMKeyServiceVersion
) : TIMKeyService {

    override fun createKey(
        scope: CoroutineScope,
        secret: String
    ): Deferred<TIMResult<TIMKeyModel, TIMKeyServiceError>> = scope.async {
        toTIMKeyServiceResult {
            api.createKey(
                version.pathValue,
                TIMKeyRequestBody.CreateKey(
                    secret
                )
            ).await()
        }
    }

    override fun getKeyViaSecret(
        scope: CoroutineScope,
        secret: String,
        keyId: String
    ): Deferred<TIMResult<TIMKeyModel, TIMKeyServiceError>> = scope.async {
        toTIMKeyServiceResult {
            api.getKeyViaSecret(
                version.pathValue,
                TIMKeyRequestBody.GetKey.ViaSecret(
                    secret,
                    keyId
                )
            ).await()
        }
    }

    override fun getKeyViaLongSecret(
        scope: CoroutineScope,
        longSecret: String,
        keyId: String
    ): Deferred<TIMResult<TIMKeyModel, TIMKeyServiceError>> = scope.async {
        toTIMKeyServiceResult {
            api.getKeyViaLongSecret(
                version.pathValue,
                TIMKeyRequestBody.GetKey.ViaLongSecret(
                    longSecret,
                    keyId
                )
            ).await()
        }
    }

    companion object {
        private var instance: TIMKeyServiceImpl? = null

        fun getInstance(
            config: TIMKeyServiceConfiguration
        ): TIMKeyServiceImpl {
            return instance ?: TIMKeyServiceImpl(
                api = Retrofit.Builder()
                    .baseUrl(config.realmBaseUrl)
                    .addConverterFactory(JsonConverter.factory)
                    .build()
                    .create(TIMKeyServiceAPI::class.java),
                version = config.version
            )
        }

    }
}
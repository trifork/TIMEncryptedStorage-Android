package com.trifork.timencryptedstorage.keyservice

import androidx.annotation.VisibleForTesting
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
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.await

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
class TIMKeyServiceImpl(
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
                    keyId,
                    secret
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

            //TODO Separate in to some setup function, this is probably nice to have when developing, but should not be in the production version
            val httpLoggingInterceptor = HttpLoggingInterceptor()
            httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            val okHttpClient = OkHttpClient.Builder().addInterceptor(httpLoggingInterceptor).build()



            return instance ?: TIMKeyServiceImpl(
                api = Retrofit.Builder()
                    .baseUrl(config.realmBaseUrl)
                    .addConverterFactory(JsonConverter.factory)
                    .client(okHttpClient)
                    .build()
                    .create(TIMKeyServiceAPI::class.java),
                version = config.version
            )
        }

    }
}
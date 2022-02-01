package com.trifork.timencryptedstorage.keyservice

import androidx.annotation.VisibleForTesting
import com.trifork.timencryptedstorage.BuildConfig
import com.trifork.timencryptedstorage.models.TIMResult
import com.trifork.timencryptedstorage.models.errors.TIMKeyServiceError
import com.trifork.timencryptedstorage.models.errors.TIMKeyServiceErrorCode
import com.trifork.timencryptedstorage.models.keyservice.TIMKeyServiceConfiguration
import com.trifork.timencryptedstorage.models.keyservice.TIMKeyServiceVersion
import com.trifork.timencryptedstorage.models.keyservice.request.TIMKeyRequestBody
import com.trifork.timencryptedstorage.models.keyservice.response.TIMKeyModel
import com.trifork.timencryptedstorage.models.toTIMKeyServiceResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
            ).timKeyServerAwait()
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
                    keyId,
                    longSecret
                )
            ).timKeyServerAwait()
        }
    }

    companion object {
        private var instance: TIMKeyServiceImpl? = null

        fun getInstance(
            config: TIMKeyServiceConfiguration
        ): TIMKeyServiceImpl {

            var okHttpClient = OkHttpClient.Builder().build()

            //Is this the correct location for this setup function?
            if (BuildConfig.BUILD_TYPE.contains("dev")) {
                val httpLoggingInterceptor = HttpLoggingInterceptor()
                httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
                okHttpClient = OkHttpClient.Builder().addInterceptor(httpLoggingInterceptor).build()
            }

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

    // The keyserver gives a 204 error code in case of: KeyLocked
    // In Retrofit standard await implementation it tries to parse the body of the response, but in case of KeyLocked the keyserver does not return a body
    // Therefore it would throw a KotlinNullPointerException instead of a HttpException, therefore not trigger the toTIMKeyServiceResult catch block.
    // We now throw a exception if the error code is 204, so the catch block runs in case of KeyLocked as well
    private suspend fun <T : Any> Call<T>.timKeyServerAwait(): T {
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation {
                cancel()
            }
            enqueue(object : Callback<T> {
                override fun onResponse(call: Call<T>, response: Response<T>) {
                    //Here we divert from the Retrofit standard await implementation by disregarding TIMKeyServiceErrorCode.KeyLocked as a successful response code
                    if (response.code() != TIMKeyServiceErrorCode.KeyLocked && response.isSuccessful) {
                        val body = response.body()
                        if (body == null) {
                            val invocation = call.request().tag(Invocation::class.java)!!
                            val method = invocation.method()
                            val e = KotlinNullPointerException("Response from " + method.declaringClass.name + '.' + method.name + " was null but response body type was declared as non-null")
                            continuation.resumeWithException(e)
                        } else {
                            continuation.resume(body)
                        }
                    } else {
                        continuation.resumeWithException(HttpException(response))
                    }
                }

                override fun onFailure(call: Call<T>, t: Throwable) {
                    continuation.resumeWithException(t)
                }
            })
        }
    }
}
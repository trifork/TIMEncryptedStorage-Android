package com.trifork.timencryptedstorage.keyservice

import com.trifork.timencryptedstorage.models.keyservice.request.TIMKeyRequestBody
import com.trifork.timencryptedstorage.models.keyservice.response.TIMKeyModel
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

private const val keyServicePath = "keyservice"
private const val keyServiceVersionPath = "keyServiceVersion"

internal object TIMKeyServiceEndpoint {
    const val CreateKey = "createkey"
    const val GetKey = "key"
}

interface TIMKeyServiceAPI {
    /**
     * Creates a new key entry that matches with the given [TIMKeyRequestBody.CreateKey.secret]
     */
    @POST("$keyServicePath/{$keyServiceVersionPath}/${TIMKeyServiceEndpoint.CreateKey}")
    fun createKey(
        @Path(keyServiceVersionPath) timKeyServiceVersion: String,
        @Body body: TIMKeyRequestBody.CreateKey
    ): Call<TIMKeyModel>

    /**
     * Gets an existing key if [TIMKeyRequestBody.GetKey.ViaSecret.keyId] is a verified match with [TIMKeyRequestBody.GetKey.ViaSecret.secret]
     */
    @POST("$keyServicePath/{$keyServiceVersionPath}/${TIMKeyServiceEndpoint.GetKey}")
    fun getKeyViaSecret(
        @Path(keyServiceVersionPath) timKeyServiceVersion: String,
        @Body body: TIMKeyRequestBody.GetKey.ViaSecret
    ): Call<TIMKeyModel>

    /**
     * Gets an existing key if [TIMKeyRequestBody.GetKey.ViaLongSecret.keyId] is a verified match with [TIMKeyRequestBody.GetKey.ViaLongSecret.secret]
     */
    @POST("$keyServicePath/{$keyServiceVersionPath}/${TIMKeyServiceEndpoint.GetKey}")
    fun getKeyViaLongSecret(
        @Path(keyServiceVersionPath) timKeyServiceVersion: String,
        @Body body: TIMKeyRequestBody.GetKey.ViaLongSecret
    ): Call<TIMKeyModel>


}


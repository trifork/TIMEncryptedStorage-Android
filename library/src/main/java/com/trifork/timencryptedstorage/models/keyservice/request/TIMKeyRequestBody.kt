package com.trifork.timencryptedstorage.models.keyservice.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed class TIMKeyRequestBody {

    @Serializable
    class CreateKey(
        @SerialName("secret") val secret: String
    ) : TIMKeyRequestBody()

    sealed class GetKey {
        @Serializable
        class ViaSecret(
            @SerialName("keyid") val keyId: String,
            @SerialName("secret") val secret: String
        )
        @Serializable
        class ViaLongSecret(
            @SerialName("keyid") val keyId: String,
            @SerialName("longsecret") val longSecret: String
        )
    }
}


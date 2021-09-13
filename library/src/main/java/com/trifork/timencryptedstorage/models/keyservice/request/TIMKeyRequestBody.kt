package com.trifork.timencryptedstorage.models.keyservice.request

import kotlinx.serialization.SerialName

sealed class TIMKeyRequestBody {

    class CreateKey(
        val secret: String
    ) : TIMKeyRequestBody()

    sealed class GetKey {
        class ViaSecret(
            @SerialName("keyId") val keyId: String,
            val secret: String
        )

        class ViaLongSecret(
            @SerialName("keyId") val keyId: String,
            @SerialName("longsecret") val longSecret: String
        )
    }
}


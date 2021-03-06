package com.trifork.timencryptedstorage.models.keyservice.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response model from key service
 * @property keyId The identifier for this encryption key
 * @property key Encryption key for keyId
 * @property longSecret longSecret used as secret when logging in with biometric protection
 */
@Serializable
data class TIMKeyModel(
    @SerialName("keyid") val keyId: String,
    @SerialName("key") val key: String,
    @SerialName("longsecret") val longSecret: String
)
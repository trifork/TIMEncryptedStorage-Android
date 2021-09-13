package com.trifork.timencryptedstorage.models.keyservice.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response model from key service
 * @param keyId The identifier for this encryption key
 * @param key Encryption key for keyId
 * @param longSecret longSecret used as secret when logging in with biometric protection
 */
// TODO: Potentially missing serialization - MFJ (10/09/2021)
data class TIMKeyModel(
    @SerialName("keyid") val keyId: String,
    val key: String,
    @SerialName("longsecret") val longSecret: String
)
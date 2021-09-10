package com.trifork.timencryptedstorage.models.keyservice

/**
 * Response model from key service
 * @param keyId The identifier for this encryption key
 * @param key Encryption key for keyId
 * @param longSecret longSecret used as secret when logging in with biometric protection
 */
data class TIMKeyModel(
    val keyId: String,
    val key: String,
    val longSecret: String? = null
)
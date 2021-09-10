package com.trifork.timencryptedstorage.models.keyservice


/**
 * The configuration for the TIMKeyService
 * @param realmBaseUrl The baseUrl for your realm, e.g. https://oidc-test.hosted.trifork.com/auth/realms/myrealm
 * @param version The key service API version of your realm.
 */
data class TIMKeyServiceConfiguration(
    val realmBaseUrl: String,
    val version: TIMKeyServiceVersion
)
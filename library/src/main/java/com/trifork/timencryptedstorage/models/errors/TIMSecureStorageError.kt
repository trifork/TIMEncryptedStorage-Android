package com.trifork.timencryptedstorage.models.errors

sealed class TIMSecureStorageError(val source: Throwable? = null) : Throwable() {

    class FailedToLoadData(val error: Throwable) : TIMSecureStorageError(error)
    class FailedToStoreData(val error: Throwable) : TIMSecureStorageError(error)
    class UnrecoverablyFailedToConvertData(val error: Throwable) : TIMSecureStorageError(error)

    //This exists in the iOS SDK. We get the authentication error earlier in the architecture and it is therefore not necessary here. JCH (05.01.22)
    //class AuthenticationFailedForData : TIMSecureStorageError()

    override val message: String?
        get() = when (this) {
            is FailedToStoreData -> "Failed to store data in secure storage: $error. "
            is FailedToLoadData -> "Failed to load data from secure storage. The userId was cleared from storage: $error"
            is UnrecoverablyFailedToConvertData -> "Failed to convert data from secure storage. The userId was cleared from storage: $error"
        }
}
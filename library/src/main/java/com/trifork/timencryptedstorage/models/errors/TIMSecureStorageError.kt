package com.trifork.timencryptedstorage.models.errors

sealed class TIMSecureStorageError(val source: Throwable? = null) : Throwable() {

    class FailedToLoadData(error: Throwable) : TIMSecureStorageError(error)
    class FailedToStoreData(error: Throwable) : TIMSecureStorageError(error)
    //TODO I have a hard time figuring out how and when this happens JCH (04.12.21)
    class AuthenticationFailedForData : TIMSecureStorageError()
}
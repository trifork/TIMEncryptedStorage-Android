package com.trifork.timencryptedstorage.models.errors

sealed class TIMSecureStorageError(val source: Throwable? = null) : Throwable() {

    // TODO: Fill out the blanks here when service is up and running - MFJ (10/09/2021)
    class FailedToLoadData(error: Throwable) : TIMSecureStorageError(error)
    class FailedToStoreData(error: Throwable) : TIMSecureStorageError(error)

    class AuthenticationFailedForData() : TIMSecureStorageError()
}
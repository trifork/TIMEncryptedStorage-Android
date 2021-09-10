package com.trifork.timencryptedstorage.models

sealed class TIMSecureStorageError : Throwable() {
    class FailedToLoadData(error: Throwable): TIMSecureStorageError()
    class FailedToStoreData(error: Throwable): TIMSecureStorageError()
}
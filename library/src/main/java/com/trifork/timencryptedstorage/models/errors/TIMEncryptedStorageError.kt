package com.trifork.timencryptedstorage.models.errors

sealed class TIMEncryptedStorageError {
    // TODO: Fill out the blanks here when service is up and running - MFJ (10/09/2021)
    class KeyServiceFailed(val error: TIMKeyServiceError): TIMEncryptedStorageError()
    class SecureStorageFailed(val error: TIMSecureStorageError): TIMEncryptedStorageError()
}
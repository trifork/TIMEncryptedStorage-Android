package com.trifork.timencryptedstorage.models.errors

sealed class TIMEncryptedStorageError : Throwable() {

    //region Encryption errors
    class FailedToEncryptData(val error: Throwable) : TIMEncryptedStorageError()
    class FailedToDecryptData(val error: Throwable) : TIMEncryptedStorageError()
    class InvalidEncryptionMethod : TIMEncryptedStorageError()
    class InvalidEncryptionKey : TIMEncryptedStorageError()
    //endregion

    //region KeySever errors
    class KeyServiceFailed(val error: TIMKeyServiceError) : TIMEncryptedStorageError()
    //endregion

    //region Secure storage errors
    class SecureStorageFailed(val error: TIMSecureStorageError) : TIMEncryptedStorageError()
    //endregion

    //region Unexpected data from secure storage
    class UnexpectedData : TIMEncryptedStorageError()
    //endregion

    override val message: String?
        get() = when (this) {
            is FailedToEncryptData -> "Failed to encrypt data with specified key: ${error.localizedMessage}"
            is KeyServiceFailed -> "The KeyService failed with error: $error"
            is SecureStorageFailed -> "The secure storage failed with error: $error"
            is FailedToDecryptData -> "Failed to decrypt data with specified key: $error"
            is InvalidEncryptionKey -> "The encryption key is invalid."
            is InvalidEncryptionMethod -> "The encryption method is invalid. Did you remember to call the configure method?"
            is UnexpectedData -> "The secure storage loaded unexpected data. Failed to use the data."
        }
}
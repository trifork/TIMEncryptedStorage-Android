package com.trifork.timencryptedstorage.models.errors

sealed class TIMEncryptedStorageError : Throwable() {

    //region Encryption errors
    class FailedToEncryptData(val error: Throwable) : TIMEncryptedStorageError()
    class FailedToDecryptData(val error: Throwable) : TIMEncryptedStorageError()
    class InvalidEncryptionMethod : TIMEncryptedStorageError()
    class InvalidEncryptionKey(val error: Throwable) : TIMEncryptedStorageError()
    class PermanentlyInvalidatedKey(val error: Throwable) : TIMEncryptedStorageError()
    class UnrecoverablyFailedToEncrypt(val error: Throwable) : TIMEncryptedStorageError()
    class UnrecoverablyFailedToDecrypt(val error: Throwable) : TIMEncryptedStorageError()
    class InvalidCipher(val error: Throwable) : TIMEncryptedStorageError()
    //endregion

    //region KeySever errors
    class KeyServiceFailed(val error: TIMKeyServiceError) : TIMEncryptedStorageError()
    class KeyServiceJWTDecodeFailed(val error: Throwable) : TIMEncryptedStorageError()
    //endregion

    //region Secure storage errors
    class SecureStorageFailed(val error: TIMSecureStorageError) : TIMEncryptedStorageError()
    //endregion

    //region Unexpected data from secure storage
    class UnexpectedData : TIMEncryptedStorageError()
    //endregion

    //region Biometric helper
    class FailedToEncodeData(val error: Throwable) : TIMEncryptedStorageError()
    class FailedToDecodeData(val error: Throwable) : TIMEncryptedStorageError()
    //endregion

    override val message: String?
        get() = when (this) {
            is FailedToEncryptData -> "Failed to encrypt data with specified key: ${error.localizedMessage}"
            is KeyServiceFailed -> "The KeyService failed with error: $error"
            is KeyServiceJWTDecodeFailed -> "The KeyService JWT could not be decoded: $error"
            is SecureStorageFailed -> "The secure storage failed with error: $error"
            is FailedToDecryptData -> "Failed to decrypt data with specified key: $error"
            is InvalidEncryptionKey -> "The encryption key is invalid: $error"
            is PermanentlyInvalidatedKey -> "Biometric enrollment was changed : $error"
            is UnrecoverablyFailedToDecrypt -> "The biometric data is in an unrecoverable state : $error"
            is UnrecoverablyFailedToEncrypt -> "The biometric data is in an unrecoverable state : $error"
            is InvalidCipher -> "The cipher could not be instantiated: $error"
            is InvalidEncryptionMethod -> "The encryption method is invalid. Did you remember to call the configure method?"
            is FailedToEncodeData -> "Encoding of the biometric encrypted data failed: $error"
            is FailedToDecodeData -> "Decoding of the stored biometric encrypted data failed: $error"
            is UnexpectedData -> "The secure storage loaded unexpected data. Failed to use the data."
        }
}
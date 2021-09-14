package com.trifork.timencryptedstorage.models

sealed class TIMESEncryptionMethod {
    object AesGcm: TIMESEncryptionMethod()
}
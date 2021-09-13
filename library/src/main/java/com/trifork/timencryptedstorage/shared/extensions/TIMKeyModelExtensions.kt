package com.trifork.timencryptedstorage.shared.extensions

import com.trifork.timencryptedstorage.models.TIMESEncryptionMethod
import com.trifork.timencryptedstorage.models.keyservice.response.TIMKeyModel

fun TIMKeyModel.encrypt(data: ByteArray, encryptionMethod: TIMESEncryptionMethod): ByteArray {
    TODO()
}
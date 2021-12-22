package com.trifork.timencryptedstorage.helpers

interface TIMEncryptedStorageLogger {
    fun log(priority: Int, tag: String, msg: String, throwable: Throwable? = null)
}
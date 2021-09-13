package com.trifork.timencryptedstorage.shared.extensions

internal val String.asPreservedByteArray: ByteArray
    get() = toByteArray(Charsets.ISO_8859_1)

internal val ByteArray.asPreservedString: String
    get() = String(this, Charsets.ISO_8859_1)
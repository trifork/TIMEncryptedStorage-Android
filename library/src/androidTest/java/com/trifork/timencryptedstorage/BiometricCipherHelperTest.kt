package com.trifork.timencryptedstorage

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.trifork.timencryptedstorage.shared.BiometricCipherHelper
import com.trifork.timencryptedstorage.shared.extensions.asPreservedByteArray
import com.trifork.timencryptedstorage.shared.extensions.asPreservedString
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BiometricCipherHelperTest {

    @Test
    fun testEncryptDecryptValue() {
        val longSecret = "longSecret"

        val encryptionCipher = BiometricCipherHelper.getInitializedCipherForEncryption()
        val encryptedLongSecret = BiometricCipherHelper.encrypt(encryptionCipher, longSecret.asPreservedByteArray)

        Assert.assertNotEquals(longSecret, encryptedLongSecret)

        val decryptionCipher = BiometricCipherHelper.getInitializedCipherForDecryption(encryptionCipher.iv)
        val decryptedLongSecret = BiometricCipherHelper.decrypt(decryptionCipher, encryptedLongSecret).asPreservedString

        Assert.assertEquals(longSecret, decryptedLongSecret)
    }

}
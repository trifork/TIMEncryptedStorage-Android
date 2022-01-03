package com.trifork.timencryptedstorage

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trifork.timencryptedstorage.models.TIMResult

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

        val encryptionCipherResult = BiometricCipherHelper.getInitializedCipherForEncryption() as TIMResult.Success
        val encryptionCipher = encryptionCipherResult.value

        val encryptedLongSecretResult = BiometricCipherHelper.encrypt(encryptionCipher, longSecret.asPreservedByteArray) as TIMResult.Success
        val encryptedLongSecret = encryptedLongSecretResult.value

        Assert.assertNotEquals(longSecret, encryptedLongSecret)

        val decryptionCipherResult = BiometricCipherHelper.getInitializedCipherForDecryption(encryptionCipher.iv) as TIMResult.Success
        val decryptionCipher = decryptionCipherResult.value

        val decryptedLongSecret = BiometricCipherHelper.decrypt(decryptionCipher, encryptedLongSecret) as TIMResult.Success
        Assert.assertEquals(longSecret, decryptedLongSecret.value.asPreservedString)
    }

}
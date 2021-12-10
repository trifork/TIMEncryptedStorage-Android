package com.trifork.timencryptedstorage

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trifork.timencryptedstorage.models.TIMESEncryptionMethod
import com.trifork.timencryptedstorage.models.TIMResult
import com.trifork.timencryptedstorage.models.errors.TIMEncryptedStorageError
import com.trifork.timencryptedstorage.models.keyservice.response.TIMKeyModel
import com.trifork.timencryptedstorage.shared.extensions.decrypt
import com.trifork.timencryptedstorage.shared.extensions.encrypt
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TIMKeyModelExtensionsTests {

    private val charset = Charsets.UTF_8
    private val timKeyModel = TIMKeyModel("168dfa8a-a613-488d-876c-1a79122c8d5a", "/RT5VXFinR27coWdsieCt3UxoKibplkO+bCVNkDJK9o=", "xe6XhucZ0BnH3yLQFR1wrZgPe3l4q/ymnQCCY/iZs3A=")

    @Test
    fun encrypt_decrypt() {
        val data = "Random-Data".toByteArray(charset)

        val encryptResult = timKeyModel.encrypt(data, TIMESEncryptionMethod.AesGcm) as TIMResult.Success
        val encryptResult2 = timKeyModel.encrypt(data, TIMESEncryptionMethod.AesGcm) as TIMResult.Success
        Assert.assertNotEquals(encryptResult.value, encryptResult2.value)

        val decryptResult = timKeyModel.decrypt(encryptResult.value, TIMESEncryptionMethod.AesGcm) as TIMResult.Success
        val decryptResult2 = timKeyModel.decrypt(encryptResult2.value, TIMESEncryptionMethod.AesGcm) as TIMResult.Success
        val decryptedData = decryptResult.value.toString(charset)
        val decryptedData2 = decryptResult2.value.toString(charset)
        Assert.assertEquals(data.toString(charset), decryptedData)
        Assert.assertEquals(data.toString(charset), decryptedData2)
    }

    @Test
    fun encrypt_invalid_key() {
        //Invalid key and long secret
        val invalidTIMKeyModel = TIMKeyModel("168dfa8a-a613-488d-876c-1a79122c8d5a", "/RT5VXFinR27coWdsie", "xe6XhucZ0BnH3yLQFR1wrZgPe3l4")
        val data = "Random-Data".toByteArray(charset)

        val encryptResult = invalidTIMKeyModel.encrypt(data, TIMESEncryptionMethod.AesGcm) as TIMResult.Failure

        Assert.assertEquals(TIMEncryptedStorageError.FailedToEncryptData::class, encryptResult.error::class)
    }

    @Test
    fun decrypt_invalid_key() {
        val timKeyModel = TIMKeyModel("168dfa8a-a613-488d-876c-1a79122c8d5a", "/RT5VXFinR27coWdsieCt3UxoKibplkO+bCVNkDJK9o=", "xe6XhucZ0BnH3yLQFR1wrZgPe3l4q/ymnQCCY/iZs3A=")
        val data = "Random-Data".toByteArray(charset)
        //Invalid not decrypted data sent to decrypt
        val decryptResult = timKeyModel.decrypt(data, TIMESEncryptionMethod.AesGcm) as TIMResult.Failure
        Assert.assertEquals(TIMEncryptedStorageError.FailedToDecryptData::class, decryptResult.error::class)
    }
}
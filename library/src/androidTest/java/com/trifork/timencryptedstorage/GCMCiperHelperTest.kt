package com.trifork.timencryptedstorage

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trifork.timencryptedstorage.models.TIMResult
import com.trifork.timencryptedstorage.models.keyservice.response.TIMKeyModel
import com.trifork.timencryptedstorage.shared.extensions.GCMCipherHelper
import com.trifork.timencryptedstorage.shared.extensions.getAesKey
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GCMCipherHelperTest {

    private val charset = Charsets.UTF_8

    @Test
    fun encrypt_decrypt() {
        val timKeyModel = TIMKeyModel("168dfa8a-a613-488d-876c-1a79122c8d5a", "/RT5VXFinR27coWdsieCt3UxoKibplkO+bCVNkDJK9o=", "xe6XhucZ0BnH3yLQFR1wrZgPe3l4q/ymnQCCY/iZs3A=")
        val data = "Random-Data".toByteArray(charset)
        val aesKey = when (val aesKeyResult = timKeyModel.getAesKey()) {
            is TIMResult.Failure -> throw RuntimeException()
            is TIMResult.Success -> {
                aesKeyResult.value
            }
        }

        val encryptResult = GCMCipherHelper.encrypt(aesKey, data)
        val encryptResult2 = GCMCipherHelper.encrypt(aesKey, data)
        Assert.assertNotEquals(encryptResult, encryptResult2)

        val decryptResult = GCMCipherHelper.decrypt(aesKey, encryptResult)
        val decryptResult2 = GCMCipherHelper.decrypt(aesKey, encryptResult2)
        val expectedResult = decryptResult.toString(charset)
        val expectedResult2 = decryptResult2.toString(charset)
        Assert.assertEquals(data.toString(charset), expectedResult)
        Assert.assertEquals(data.toString(charset), expectedResult2)
    }


}
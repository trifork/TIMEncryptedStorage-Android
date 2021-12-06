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

    val charset = Charsets.UTF_8

    @Test
    fun encrypt_decrypt() {
        val timKeyModel = TIMKeyModel("168dfa8a-a613-488d-876c-1a79122c8d5a", "/RT5VXFinR27coWdsieCt3UxoKibplkO+bCVNkDJK9o=", "xe6XhucZ0BnH3yLQFR1wrZgPe3l4q/ymnQCCY/iZs3A=")
        val data = "hej med dig".toByteArray(charset)
        val aesKeyResult = timKeyModel.getAesKey()
        val aesKey = when (aesKeyResult) {
            is TIMResult.Failure -> throw RuntimeException()
            is TIMResult.Success -> {
                aesKeyResult.value
            }
        }
        val encryptResult = GCMCipherHelper.encrypt(aesKey, data)
        val decryptResult = GCMCipherHelper.decrypt(aesKey, encryptResult)
        val expectedResult = decryptResult.toString(charset)
        Assert.assertEquals(data.toString(charset), expectedResult)
    }


}
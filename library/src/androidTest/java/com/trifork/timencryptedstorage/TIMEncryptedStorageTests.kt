package com.trifork.timencryptedstorage

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trifork.timencryptedstorage.helpers.test.SecureStorageMock
import com.trifork.timencryptedstorage.helpers.test.TIMEncryptedStorageLoggerInternal
import com.trifork.timencryptedstorage.helpers.test.TIMKeyServiceStub
import com.trifork.timencryptedstorage.models.TIMESEncryptionMethod
import com.trifork.timencryptedstorage.models.TIMResult
import com.trifork.timencryptedstorage.models.errors.TIMEncryptedStorageError
import com.trifork.timencryptedstorage.models.errors.TIMSecureStorageError
import com.trifork.timencryptedstorage.shared.BiometricCipherHelper
import com.trifork.timencryptedstorage.shared.extensions.asPreservedByteArray
import com.trifork.timencryptedstorage.shared.extensions.asPreservedString
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import javax.crypto.Cipher

@RunWith(AndroidJUnit4::class)
class TIMEncryptedStorageTests {

    private val testStore = SecureStorageMock()
    private val testKeyService = TIMKeyServiceStub()
    private val biometricCipherHelper = BiometricCipherHelper
    private val id = "test"
    private val data = "testData".asPreservedByteArray
    private val secret = "1234"
    private val longSecret = "longSecret"
    private val keyId = "123456789"

    @Test
    fun testHasValue() = runBlocking {
        val storage = createTIMEncryptedStorage()

        Assert.assertFalse(storage.hasValue(id))

        storage.store(this, id, data, keyId, secret).await()
        Assert.assertTrue(storage.hasValue(id))

        storage.remove(id)
        Assert.assertFalse(storage.hasValue(id))

        val enableResult = storage.enableBiometric(this, keyId, secret, getEncryptionCipher("")).await()

        Assert.assertEquals(TIMResult.Success::class, enableResult::class)

        val storeViaBiometricResult = storage.storeViaBiometric(this, id, data, testKeyService.keyId).await()
        Assert.assertEquals(TIMResult.Success::class, storeViaBiometricResult::class)

        val hasVal = storage.hasValue(id)
        Assert.assertTrue(hasVal)
    }

    @Test
    fun testHasBiometricProtectedValue() = runBlocking {
        val storage = createTIMEncryptedStorage()

        Assert.assertFalse(storage.hasBiometricProtectedValue(id, keyId))

        storage.store(this, id, data, keyId, secret).await()

        Assert.assertFalse(storage.hasBiometricProtectedValue(id, keyId)) //Not bio protected!
        storage.remove(id)

        val enableBioResult = storage.enableBiometric(this, keyId, secret, getEncryptionCipher("")).await()

        Assert.assertEquals(TIMResult.Success::class, enableBioResult::class)

        val storeViaBioResult = storage.storeViaBiometric(this, id, data, testKeyService.keyId).await()

        Assert.assertEquals(TIMResult.Success::class, storeViaBioResult::class)

        Assert.assertTrue(storage.hasBiometricProtectedValue(id, testKeyService.keyId))
        Assert.assertFalse(storage.hasBiometricProtectedValue(id, "NotTheRightKeyId"))
    }

    @Test
    fun testRemoveLongSecret() = runBlocking {
        val storage = createTIMEncryptedStorage()
        val encryptionCipherOne = getEncryptionCipher("1")
        val encryptionCipherTwo = getEncryptionCipher("2")
        val decryptionCipherOne = getDecryptionCipher("1", encryptionCipherOne.iv)
        val decryptionCipherTwo = getDecryptionCipher("2", encryptionCipherTwo.iv)

        val enableBioResult = storage.enableBiometric(this, keyId, secret, encryptionCipherOne).await()
        Assert.assertEquals(TIMResult.Success::class, enableBioResult::class)

        val storeViaBioResult = storage.storeViaBiometricWithNewKey(this, id, data, testKeyService.longSecret, encryptionCipherTwo).await() as TIMResult.Success
        Assert.assertEquals(TIMResult.Success::class, storeViaBioResult::class)

        // Get it via bio to make sure it is accessible via the used longSecret
        val getViaBioResult = storage.getViaBiometric(this, id, testKeyService.keyId, decryptionCipherOne).await() as TIMResult.Success

        Assert.assertEquals(testKeyService.longSecret, getViaBioResult.value.longSecret)
        Assert.assertEquals(data.asPreservedString, getViaBioResult.value.data.asPreservedString)

        // Remove!
        storage.removeLongSecret(testKeyService.keyId)

        // It should be inaccessible with the longSecret now ...
        val getViaBioFailureResult = storage.getViaBiometric(this, id, testKeyService.keyId, decryptionCipherTwo).await() as TIMResult.Failure
        val error = getViaBioFailureResult.error as TIMEncryptedStorageError.SecureStorageFailed
        Assert.assertEquals(TIMSecureStorageError.FailedToLoadData::class, error.error::class)
    }

    @Test
    fun testStoreAndLoadData() = runBlocking {
        val storage = createTIMEncryptedStorage()

        val storeResult = storage.store(this, id, data, keyId, secret).await()
        Assert.assertEquals(TIMResult.Success::class, storeResult::class)

        val savedData = testStore.get(keyId)
        Assert.assertNotEquals(data, savedData) //The data we saved should not be equal to the data in the storage. Should be encrypted.

        val loadedDataResult = storage.get(this, id, keyId, secret).await() as TIMResult.Success

        Assert.assertEquals(data.asPreservedString, loadedDataResult.value.asPreservedString)
    }

    @Test
    fun testStoreAndLoadWithLongSecretData() = runBlocking {
        val storage = createTIMEncryptedStorage()

        val storeResult = storage.store(this, id, data, keyId, longSecret).await()
        Assert.assertEquals(TIMResult.Success::class, storeResult::class)

        val expectedLoad = storage.get(this, id, keyId, longSecret).await() as TIMResult.Success
        Assert.assertEquals(data.asPreservedString, expectedLoad.value.asPreservedString)
    }

    @Test
    fun testStoreWithNewKey() = runBlocking {
        val storage = createTIMEncryptedStorage()

        val storeResult = storage.storeWithNewKey(this, id, data, longSecret).await() as TIMResult.Success

        Assert.assertEquals(testKeyService.keyId, storeResult.value.keyId)
        Assert.assertEquals(testKeyService.longSecret, storeResult.value.longSecret)

        val storedValue = storage.get(this, id, storeResult.value.keyId, secret).await() as TIMResult.Success

        Assert.assertEquals(data.asPreservedString, storedValue.value.asPreservedString)
    }

    @Test
    fun testStoreViaBiometricWithNewKey() = runBlocking {
        val storage = createTIMEncryptedStorage()
        val encryptionCipherOne = getEncryptionCipher("")
        val decryptionCipherTwo = getDecryptionCipher("", encryptionCipherOne.iv)

        val storeViaBioResult = storage.storeViaBiometricWithNewKey(this, id, data, secret, encryptionCipherOne).await() as TIMResult.Success

        Assert.assertEquals(testKeyService.keyId, storeViaBioResult.value.keyId)
        Assert.assertEquals(testKeyService.longSecret, storeViaBioResult.value.longSecret)

        val loadedResult = storage.getViaBiometric(this, id, storeViaBioResult.value.keyId, decryptionCipherTwo).await() as TIMResult.Success

        Assert.assertEquals(loadedResult.value.data.asPreservedString, data.asPreservedString)
        Assert.assertEquals(loadedResult.value.longSecret, testKeyService.longSecret)

    }

    //region private helpers
    private fun createTIMEncryptedStorage(): TIMEncryptedStorage = TIMEncryptedStorage(
        testStore,
        TIMEncryptedStorageLoggerInternal(),
        testKeyService,
        TIMESEncryptionMethod.AesGcm
    )

    private fun getEncryptionCipher(keyId: String) : Cipher {
        val encryptionCipher = biometricCipherHelper.getInitializedCipherForEncryption(keyId) as TIMResult.Success
        return encryptionCipher.value
    }

    private fun getDecryptionCipher(keyId: String, initializationVector: ByteArray) : Cipher {
        val encryptionCipher = biometricCipherHelper.getInitializedCipherForDecryption(keyId, initializationVector) as TIMResult.Success
        return encryptionCipher.value
    }

    //endregion
}

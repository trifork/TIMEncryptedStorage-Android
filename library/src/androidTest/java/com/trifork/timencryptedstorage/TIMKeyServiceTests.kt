package com.trifork.timencryptedstorage

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trifork.timencryptedstorage.keyservice.TIMKeyServiceAPI
import com.trifork.timencryptedstorage.keyservice.TIMKeyServiceImpl
import com.trifork.timencryptedstorage.models.TIMResult
import com.trifork.timencryptedstorage.models.errors.TIMKeyServiceError
import com.trifork.timencryptedstorage.models.errors.TIMKeyServiceErrorCode.Companion.PotentiallyNoInternet
import com.trifork.timencryptedstorage.models.keyservice.TIMKeyServiceVersion
import com.trifork.timencryptedstorage.models.keyservice.response.TIMKeyModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.HttpException
import retrofit2.await

@RunWith(AndroidJUnit4::class)
class TIMKeyServiceTests {

    @Test
    fun createKey() = runBlocking {
        val service = TIMKeyServiceImpl(
            stubbedControllerHelperTIMKeyModel(),
            TIMKeyServiceVersion.V1
        )
        val createKeyResult = service.createKey(this, "").await() as TIMResult.Success

        Assert.assertEquals(keyId, createKeyResult.value.keyId)
        Assert.assertEquals(key, createKeyResult.value.key)
        Assert.assertEquals(longSecret, createKeyResult.value.longSecret)
    }

    @Test
    fun getKeyViaSecret() = runBlocking {
        val service = TIMKeyServiceImpl(
            stubbedControllerHelperTIMKeyModel(),
            TIMKeyServiceVersion.V1
        )
        val getKeyViaSecretResult = service.getKeyViaSecret(this, "", "").await() as TIMResult.Success

        Assert.assertEquals(keyId, getKeyViaSecretResult.value.keyId)
        Assert.assertEquals(key, getKeyViaSecretResult.value.key)
        Assert.assertEquals(longSecret, getKeyViaSecretResult.value.longSecret)
    }

    @Test
    fun getKeyViaLongSecret() = runBlocking {
        val service = TIMKeyServiceImpl(
            stubbedControllerHelperTIMKeyModel(),
            TIMKeyServiceVersion.V1
        )
        val getKeyViaLongSecretResult = service.getKeyViaLongSecret(this, "", "").await() as TIMResult.Success

        Assert.assertEquals(keyId, getKeyViaLongSecretResult.value.keyId)
        Assert.assertEquals(key, getKeyViaLongSecretResult.value.key)
        Assert.assertEquals(longSecret, getKeyViaLongSecretResult.value.longSecret)
    }

    @Test
    fun createKeyHandleTIMKeyServiceErrorCode() = runBlocking {
        val service = TIMKeyServiceImpl(
            stubbedControllerHelperThrowHttpException(PotentiallyNoInternet),
            TIMKeyServiceVersion.V1
        )
        val createKeyResult = service.createKey(this, "").await() as TIMResult.Failure

        Assert.assertEquals(TIMKeyServiceError.PotentiallyNoInternet::class, createKeyResult.error::class)
    }

    @Test
    fun createKeyHandleRuntimeException() = runBlocking {
        val service = TIMKeyServiceImpl(
            stubbedControllerHelperThrowRuntimeException(),
            TIMKeyServiceVersion.V1
        )
        val createKeyResult = service.createKey(this, "").await() as TIMResult.Failure
        val failure = createKeyResult.error as TIMKeyServiceError.Unknown

        Assert.assertEquals(TIMKeyServiceError.Unknown::class, failure::class)
        Assert.assertEquals(runTimeExceptionErrorDescription, failure.error.message)
    }

    //region helper methods

    private val keyId = "keyId"
    private val key = "key"
    private val longSecret = "longSecret"
    private val runTimeExceptionErrorDescription = "A runtime exception"

    private fun stubbedControllerHelperTIMKeyModel(): TIMKeyServiceAPI {
        //Necessary for mockk not to hang indefinitely when calling await in below stub functions
        mockkStatic("retrofit2.KotlinExtensions")

        //We stub all three methods in TIMKeyServiceAPI, returning a TIMKeyModel if the version is V1 and a second any parameter.
        return mockk {
            coEvery {
                getKeyViaLongSecret(TIMKeyServiceVersion.V1.pathValue, any()).await()
            } returns TIMKeyModel(keyId, key, longSecret)

            coEvery {
                getKeyViaSecret(TIMKeyServiceVersion.V1.pathValue, any()).await()
            } returns TIMKeyModel(keyId, key, longSecret)

            coEvery {
                createKey(TIMKeyServiceVersion.V1.pathValue, any()).await()
            } returns TIMKeyModel(keyId, key, longSecret)
        }
    }

    private fun stubbedControllerHelperThrowHttpException(returnCode: Int): TIMKeyServiceAPI {
        //We stub the code call on the returned HttpException to return the input returnCode
        val codeModel = mockk<HttpException> {
            every {
                code()
            } returns returnCode
        }

        //We stub the createKey call, returning the above defined HttpException
        return mockk {
            coEvery {
                createKey(TIMKeyServiceVersion.V1.pathValue, any())
            } throws codeModel
        }
    }

    private fun stubbedControllerHelperThrowRuntimeException(): TIMKeyServiceAPI {
        //We stub the createKey call, throwing a RuntimeException
        return mockk {
            coEvery {
                createKey(TIMKeyServiceVersion.V1.pathValue, any())
            } throws RuntimeException(runTimeExceptionErrorDescription)
        }
    }

    //endregion
}


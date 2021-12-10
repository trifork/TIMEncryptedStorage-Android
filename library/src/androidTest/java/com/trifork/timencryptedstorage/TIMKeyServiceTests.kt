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
    fun createKey_handle_error() = runBlocking {
        val service = TIMKeyServiceImpl(
            mockedControllerHelperThrowHttpException(PotentiallyNoInternet),
            TIMKeyServiceVersion.V1
        )
        val createKeyResult = service.createKey(this, "").await() as TIMResult.Failure

        Assert.assertEquals(TIMKeyServiceError.PotentiallyNoInternet::class, createKeyResult.error::class)
    }

    @Test
    fun createKey_success() = runBlocking {
        val service = TIMKeyServiceImpl(
            mockedControllerHelperReturn(),
            TIMKeyServiceVersion.V1
        )
        val createKeyResult = service.createKey(this, "").await() as TIMResult.Success

        Assert.assertEquals("keyId", createKeyResult.value.keyId)
    }

    private fun mockedControllerHelperReturn(): TIMKeyServiceAPI {
        mockkStatic("retrofit2.KotlinExtensions")
        return mockk<TIMKeyServiceAPI> {
            coEvery {
                createKey(TIMKeyServiceVersion.V1.pathValue, any()).await()
            } returns TIMKeyModel("keyId", "", "")
        }
    }

    private fun mockedControllerHelperThrowHttpException(returnCode: Int): TIMKeyServiceAPI {
        val codeModel = mockk<HttpException> {
            every {
                code()
            } returns returnCode
        }

        return mockk<TIMKeyServiceAPI> {
            coEvery {
                getKeyViaSecret(any(), any())
                createKey(TIMKeyServiceVersion.V1.pathValue, any())
            } throws codeModel
        }
    }

}


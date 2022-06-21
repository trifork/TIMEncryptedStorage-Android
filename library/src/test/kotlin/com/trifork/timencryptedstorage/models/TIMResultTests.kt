package com.trifork.timencryptedstorage.models

import com.trifork.timencryptedstorage.models.errors.*
import org.junit.*

class TIMResultTests {

    @Test
    fun toTIMSuccess() {
        val success: TIMResult.Success<String> = "test".toTIMSuccess()
        Assert.assertEquals("test", success.value)
    }

    @Test
    fun toTIMFailure() {
        val failed: TIMResult.Failure<Error> = Error("some message").toTIMFailure()
        Assert.assertEquals("some message", failed.error.message)
    }


    class ToTIMKeyServiceResult {
        @Test
        fun onFailed() {
            val shouldFailResult = toTIMKeyServiceResult {
                throw Error("reason")
            }
            Assert.assertTrue(shouldFailResult is TIMResult.Failure)
            shouldFailResult as TIMResult.Failure
            Assert.assertTrue(shouldFailResult.error is TIMKeyServiceError.Unknown)
            val serviceError = shouldFailResult.error as TIMKeyServiceError.Unknown
            Assert.assertNotNull(serviceError)
            Assert.assertEquals("reason", serviceError.error.message)
        }

        @Test
        fun success() {
            val shouldSucceed = toTIMKeyServiceResult {
                42
            }
            Assert.assertTrue(shouldSucceed is TIMResult.Success)
            shouldSucceed as TIMResult.Success
            Assert.assertEquals(42, shouldSucceed.value)
        }
    }

    @Test
    fun isFailed() {
        val success: TIMResult<Int, Error> = TIMResult.Success(42)
        val failed: TIMResult<Int, Error> = TIMResult.Failure(Error("reason"))

        Assert.assertTrue(failed.isFailed())
        Assert.assertFalse(success.isFailed())
    }

    @Test
    fun isSuccess() {
        val success: TIMResult<Int, Error> = TIMResult.Success(42)
        val failed: TIMResult<Int, Error> = TIMResult.Failure(Error("reason"))

        Assert.assertFalse(failed.isSuccess())
        Assert.assertTrue(success.isSuccess())
    }

    @Test
    fun mapValueOrOnFailedTest() {
        val success: TIMResult<Int, Error> = TIMResult.Success(42)

        val successValue = success.mapValueOrOnFailed {
            throw Error("should not be called")
        }

        Assert.assertEquals(42, successValue)

        val failed: TIMResult<Int, Error> = TIMResult.Failure(Error("reason"))
        failed.mapValueOrOnFailed {
            Assert.assertEquals("reason", it.message)
            return@mapValueOrOnFailedTest
        }

        Assert.fail("should not be called")
    }
}
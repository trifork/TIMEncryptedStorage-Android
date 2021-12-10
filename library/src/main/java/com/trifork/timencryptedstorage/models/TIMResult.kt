package com.trifork.timencryptedstorage.models

import com.trifork.timencryptedstorage.models.errors.TIMKeyServiceError
import com.trifork.timencryptedstorage.models.errors.mapToTIMKeyServiceError

sealed class TIMResult<out Value, out Failure> {
    class Success<out Value>(val value: Value) : TIMResult<Value, Nothing>()
    class Failure<out Failure>(val error: Failure) : TIMResult<Nothing, Failure>()

    companion object
}

fun <T> T.toTIMSuccess(): TIMResult.Success<T> = TIMResult.Success(this)

fun <Error: Throwable> Error.toTIMFailure() = TIMResult.Failure(this)

inline fun <Value> toTIMKeyServiceResult(block: () -> Value): TIMResult<Value, TIMKeyServiceError> =
    try {
        block().toTIMSuccess()
    } catch (e: Throwable) {
        e.mapToTIMKeyServiceError().toTIMFailure()
    }
package com.trifork.timencryptedstorage.models

import com.trifork.timencryptedstorage.models.errors.TIMKeyServiceError
import com.trifork.timencryptedstorage.models.errors.mapToTIMKeyServiceError

sealed class TIMResult<out Value, out Failure> {
    class Success<out Value>(val value: Value) : TIMResult<Value, Nothing>()
    class Failure<out Failure>(val error: Failure) : TIMResult<Nothing, Failure>()
}

fun <Value, Error> Value.toTIMSucces(): TIMResult<Value, Error> = TIMResult.Success(this)

inline fun <Value> toTIMKeyServiceResult(block: () -> Value): TIMResult<Value, TIMKeyServiceError> =
    try {
        block().toTIMSucces()
    } catch (e: Throwable) {
        TIMResult.Failure(e.mapToTIMKeyServiceError())
    }
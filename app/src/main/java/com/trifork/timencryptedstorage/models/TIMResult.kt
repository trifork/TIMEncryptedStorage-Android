package com.trifork.timencryptedstorage.models

sealed class TIMResult<out Success, out Failure> {
    class Success<out Success>(value: Success): TIMResult<Success, Nothing>()
    class Failure<out Failure>(error: Failure): TIMResult<Nothing, Failure>()
}

fun <Value, Error> Value.toTIMSucces(): TIMResult<Value, Error> = TIMResult.Success(this)
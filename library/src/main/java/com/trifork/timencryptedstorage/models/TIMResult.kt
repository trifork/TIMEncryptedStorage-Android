@file:OptIn(ExperimentalContracts::class)
@file:Suppress("NOTHING_TO_INLINE")

package com.trifork.timencryptedstorage.models

import com.trifork.timencryptedstorage.models.errors.*
import kotlin.contracts.*
import kotlin.math.*

sealed class TIMResult<out Value, out Failure> {
    class Success<out Value>(val value: Value) : TIMResult<Value, Nothing>()
    class Failure<out Failure>(val error: Failure) : TIMResult<Nothing, Failure>()

    companion object
}

fun <T> T.toTIMSuccess(): TIMResult.Success<T> = TIMResult.Success(this)

fun <Error : Throwable> Error.toTIMFailure() = TIMResult.Failure(this)

/**
 * Tries to execute the [block] and returns a success (if it returns)
 * If it throws then the exception is caught and converted to a key service error.
 * @param block Function0<Value>
 * @return TIMResult<Value, TIMKeyServiceError>
 */
inline fun <Value> toTIMKeyServiceResult(block: () -> Value): TIMResult<Value, TIMKeyServiceError> =
    try {
        block().toTIMSuccess()
    } catch (e: Throwable) {
        e.mapToTIMKeyServiceError().toTIMFailure()
    }


inline fun <Value, Error> TIMResult<Value, Error>.isFailed(): Boolean {
    contract {
        returns(true) implies (this@isFailed is TIMResult.Failure<Error>)
        returns(false) implies (this@isFailed is TIMResult.Success<Value>)
    }
    return this is TIMResult.Failure
}


inline fun <Value, Error> TIMResult<Value, Error>.isSuccess(): Boolean {
    contract {
        returns(false) implies (this@isSuccess is TIMResult.Failure<Error>)
        returns(true) implies (this@isSuccess is TIMResult.Success<Value>)
    }
    return this is TIMResult.Success
}

@Deprecated(
    message = "asking if a success result isSuccess is always true",
    replaceWith = ReplaceWith("true"),
    level = DeprecationLevel.ERROR
)
@Throws(Error::class)
@Suppress("UNUSED", "UNUSED_PARAMETER")
inline fun TIMResult.Success<*>.isSuccess(): Nothing = throw Error("Unexpected")

@Deprecated(
    message = "asking if a failed result isSuccess is always false",
    replaceWith = ReplaceWith("false"),
    level = DeprecationLevel.ERROR
)
@Throws(Error::class)
@Suppress("UNUSED", "UNUSED_PARAMETER")
inline fun TIMResult.Failure<*>.isSuccess(): Nothing = throw Error("Unexpected")


@Deprecated(
    message = "asking if a success result isFailed is always false",
    replaceWith = ReplaceWith("false"),
    level = DeprecationLevel.ERROR
)
@Throws(Error::class)
@Suppress("UNUSED", "UNUSED_PARAMETER")
inline fun TIMResult.Success<*>.isFailed(): Nothing = throw Error("Unexpected")

@Deprecated(
    message = "asking if a failed result isFailed is always true",
    replaceWith = ReplaceWith("true"),
    level = DeprecationLevel.ERROR
)
@Throws(Error::class)
@Suppress("UNUSED", "UNUSED_PARAMETER")
inline fun TIMResult.Failure<*>.isFailed(): Nothing = throw Error("Unexpected")


inline fun <Value, Error> TIMResult<Value, Error>.mapValueOrOnFailed(
    onFailed: (Error) -> Nothing
): Value = mapValueOrOnFailedResult {
    onFailed(it.error)
}

inline fun <Value, Error> TIMResult<Value, Error>.mapValueOrOnFailedResult(
    onFailed: (TIMResult.Failure<Error>) -> Nothing
): Value {
    if (this.isSuccess()) {
        return value
    } else {
        onFailed(this)
    }
}

@Deprecated(
    message = "trying to map a value on a compile time known success is redundant. use value instead",
    replaceWith = ReplaceWith("value"),
    level = DeprecationLevel.ERROR
)
@Throws(Error::class)
@Suppress("UNUSED", "UNUSED_PARAMETER")
inline fun TIMResult.Success<*>.mapValueOrOnFailed(): Nothing = throw Error("Unexpected")

@Deprecated(
    message = "trying to map a value on a compile time known failure will always call the block. ",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("block()")
)
@Throws(Error::class)
@Suppress("UNUSED", "UNUSED_PARAMETER")
inline fun TIMResult.Failure<*>.mapValueOrOnFailed(block: (Error) -> Nothing): Nothing = throw Error("Unexpected")

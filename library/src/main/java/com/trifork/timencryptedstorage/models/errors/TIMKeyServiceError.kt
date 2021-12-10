package com.trifork.timencryptedstorage.models.errors

import com.trifork.timencryptedstorage.models.errors.TIMKeyServiceErrorCode.Companion.BadPassword
import com.trifork.timencryptedstorage.models.errors.TIMKeyServiceErrorCode.Companion.KeyLocked
import com.trifork.timencryptedstorage.models.errors.TIMKeyServiceErrorCode.Companion.KeyLocked2
import com.trifork.timencryptedstorage.models.errors.TIMKeyServiceErrorCode.Companion.KeyMissing
import com.trifork.timencryptedstorage.models.errors.TIMKeyServiceErrorCode.Companion.PotentiallyNoInternet
import com.trifork.timencryptedstorage.models.errors.TIMKeyServiceErrorCode.Companion.UnableToCreateKey
import retrofit2.HttpException

sealed class TIMKeyServiceError: Throwable() {
    class BadPassword(): TIMKeyServiceError()
    class KeyLocked(): TIMKeyServiceError()
    class KeyMissing(): TIMKeyServiceError()
    class UnableToCreateKey(): TIMKeyServiceError()
    class BadInternet(): TIMKeyServiceError()
    class PotentiallyNoInternet(): TIMKeyServiceError()
    //TODO: Which of these two are correct? - JHE (09/12/2021)
    class UnableToParse(): TIMKeyServiceError()
    class UnableToDecode(val error: Throwable): TIMKeyServiceError()
    class Unknown(val error: Throwable): TIMKeyServiceError()
}

sealed class TIMKeyServiceErrorCode {
    companion object {
        const val PotentiallyNoInternet = -1009
        const val BadPassword = 401
        const val KeyLocked = 204
        const val KeyLocked2 = 403
        const val KeyMissing = 404
        const val UnableToCreateKey = 500
        const val NewErrorCode = 500
    }
}

fun Throwable.mapToTIMKeyServiceError(): TIMKeyServiceError {
    if(this is HttpException) {
        return when(val code = this.code()) {
            PotentiallyNoInternet -> TIMKeyServiceError.PotentiallyNoInternet()
            BadPassword -> TIMKeyServiceError.BadPassword()
            KeyLocked, KeyLocked2 -> TIMKeyServiceError.KeyLocked()
            KeyMissing -> TIMKeyServiceError.KeyMissing()
            UnableToCreateKey -> TIMKeyServiceError.UnableToCreateKey()
            else -> if(code < 0) TIMKeyServiceError.BadInternet() else TIMKeyServiceError.Unknown(this)
        }
    }
    return TIMKeyServiceError.Unknown(this)
}



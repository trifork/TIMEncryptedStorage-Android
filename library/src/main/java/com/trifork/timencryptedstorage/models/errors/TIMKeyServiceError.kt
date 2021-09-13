package com.trifork.timencryptedstorage.models.errors

import retrofit2.HttpException

sealed class TIMKeyServiceError {
    // TODO: Fill out the blanks here when service is up and running - MFJ (10/09/2021)
    class UnableToParse(val error: Throwable): TIMKeyServiceError()
    class Unknown(val error: Throwable): TIMKeyServiceError()
}

// TODO: Fill out the blanks here when service is up and running - MFJ (10/09/2021)
fun Throwable.mapToTIMKeyServiceError(): TIMKeyServiceError = TIMKeyServiceError.Unknown(this)



package com.trifork.timencryptedstorage.models.errors


sealed class TIMKeyServiceError: Throwable() {
    // TODO: Fill out the blanks here when service is up and running - MFJ (10/09/2021)
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

// TODO: Fill out the blanks here when service is up and running - MFJ (10/09/2021)
fun Throwable.mapToTIMKeyServiceError(): TIMKeyServiceError = TIMKeyServiceError.Unknown(this)



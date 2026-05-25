package io.github.inegru.chargebook.shared.result

/**
 * Marker interface for typed errors used with [Result]. Errors are usually enums
 * or sealed interfaces describing the discrete failure modes of a layer.
 */
interface Error

sealed interface Result<out D, out E : Error> {
    data class Success<out D>(val data: D) : Result<D, Nothing>
    data class Error<out E : io.github.inegru.chargebook.shared.result.Error>(
        val error: E,
    ) : Result<Nothing, E>
}

typealias EmptyResult<E> = Result<Unit, E>

inline fun <T, E : Error, R> Result<T, E>.map(transform: (T) -> R): Result<R, E> =
    when (this) {
        is Result.Error -> Result.Error(error)
        is Result.Success -> Result.Success(transform(data))
    }

inline fun <T, E : Error> Result<T, E>.onSuccess(action: (T) -> Unit): Result<T, E> =
    apply { if (this is Result.Success) action(data) }

inline fun <T, E : Error> Result<T, E>.onFailure(action: (E) -> Unit): Result<T, E> =
    apply { if (this is Result.Error) action(error) }

fun <T, E : Error> Result<T, E>.asEmptyResult(): EmptyResult<E> = map { }

package com.rafaelfo.labelfollower.integrations.httputils

// A fully-read HTTP response: the body is already consumed and the connection released, so
// callers never have to remember to close anything.
data class HttpResult(
    val status: Int,
    val body: String,
    val retryAfterSeconds: Long? = null,
) {
    val isSuccessful: Boolean get() = status in SUCCESS_RANGE

    private companion object {
        val SUCCESS_RANGE = 200..299
    }
}

// Thrown by RafaHttp for any response that is still not 2xx after retries. Carries the status
// and body so callers (and, eventually, an HTTP error mapping) can tell "not found" from
// "Spotify is down" instead of failing later on a half-parsed error body.
class ExternalServiceException(
    val method: String,
    val url: String,
    val status: Int,
    val responseBody: String,
) : RuntimeException("$method $url failed with HTTP $status: ${responseBody.take(MAX_BODY_IN_MESSAGE)}") {

    private companion object {
        const val MAX_BODY_IN_MESSAGE = 500
    }
}

package com.rafaelfo.labelfollower.integrations.httputils

import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration

// Thin OkHttp wrapper shared by the Spotify gateways. Every call:
//   - resolves `path` under the base `url` (which may carry its own path or port),
//   - reads and closes the response before returning it as an HttpResult,
//   - retries 429 Too Many Requests a few times, honouring Retry-After — and, for GETs only
//     (idempotent), transient 502/503/504s; a POST that may have created a playlist is never replayed,
//   - throws ExternalServiceException for anything that still isn't 2xx,
//   - logs method, URL and status only — never headers, which carry client secrets and tokens.
@Component
class RafaHttp {

    private val client = OkHttpClient()

    fun post(
        url: String,
        path: String = "",
        formBody: Map<String, String> = emptyMap(),
        body: Map<String, Any?> = emptyMap(),
        headers: Map<String, String>,
    ): HttpResult {
        val requestBody = if (body.isNotEmpty()) body.toJson().toRequestBody(JSON) else formBody.toFormBody()
        return execute(requestTo(url, path, headers).post(requestBody).build())
    }

    fun get(
        url: String,
        path: String,
        headers: Map<String, String>,
        queryParameters: Map<String, String> = emptyMap(),
    ): HttpResult = execute(requestTo(url, path, headers, queryParameters).get().build())

    private fun requestTo(
        baseUrl: String,
        path: String,
        headers: Map<String, String>,
        queryParameters: Map<String, String> = emptyMap(),
    ): Request.Builder {
        val url = baseUrl.toHttpUrl().newBuilder().run {
            if (path.isNotEmpty()) addPathSegments(path.trimStart('/'))
            queryParameters.forEach { (name, value) -> addQueryParameter(name, value) }
            build()
        }

        return Request.Builder().url(url).apply {
            headers.forEach { (name, value) -> addHeader(name, value) }
        }
    }

    private fun execute(request: Request): HttpResult {
        var result = send(request)
        var retries = 0

        while (request.isRetryable(result) && retries < MAX_RETRIES) {
            retries++
            Thread.sleep(result.retryAfter().toMillis())
            result = send(request)
        }

        if (!result.isSuccessful) {
            throw ExternalServiceException(request.method, request.url.toString(), result.status, result.body)
        }
        return result
    }

    private fun send(request: Request): HttpResult {
        val startedAt = System.nanoTime()
        val result = client.newCall(request).execute().use { response ->
            HttpResult(
                status = response.code,
                body = response.body?.string().orEmpty(),
                retryAfterSeconds = response.header("Retry-After")?.toLongOrNull(),
            )
        }
        val elapsedMs = Duration.ofNanos(System.nanoTime() - startedAt).toMillis()
        logger.info("{} {} -> {} ({} ms)", request.method, request.url, result.status, elapsedMs)
        return result
    }

    private fun Request.isRetryable(result: HttpResult): Boolean =
        result.status == TOO_MANY_REQUESTS || (method == "GET" && result.status in TRANSIENT_SERVER_ERRORS)

    private fun HttpResult.retryAfter(): Duration =
        Duration.ofSeconds((retryAfterSeconds ?: DEFAULT_RETRY_AFTER_SECONDS).coerceIn(0, MAX_RETRY_AFTER_SECONDS))

    companion object {
        private val logger = LoggerFactory.getLogger(RafaHttp::class.java)
        private val JSON = "application/json; charset=utf-8".toMediaType()

        private const val TOO_MANY_REQUESTS = 429
        private val TRANSIENT_SERVER_ERRORS = setOf(502, 503, 504)
        private const val MAX_RETRIES = 3
        private const val DEFAULT_RETRY_AFTER_SECONDS = 1L
        private const val MAX_RETRY_AFTER_SECONDS = 30L
    }
}

private fun Map<String, String>.toFormBody(): RequestBody =
    FormBody.Builder().run {
        forEach { (name, value) -> add(name, value) }
        build()
    }

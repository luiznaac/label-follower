package com.rafaelfo.labelfollower.integrations.httputils

import com.rafaelfo.labelfollower.usecases.telemetry.EventLevel
import com.rafaelfo.labelfollower.usecases.telemetry.TelemetryAttributes
import com.rafaelfo.labelfollower.usecases.telemetry.event
import okhttp3.Call
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class RafaHttp(
    private val client: Call.Factory,
) {

    private val log = LoggerFactory.getLogger(RafaHttp::class.java)

    fun post(
        url: String,
        formBody: Map<String, String> = emptyMap(),
        body: Map<String, Any?> = emptyMap(),
        headers: Map<String, String>,
    ): Response {
        val request = Request.Builder().run {
            url(url)
            headers.forEach {
                addHeader(it.key, it.value)
            }
            post(
                if (body.isNotEmpty()) {
                    body.toJson().toRequestBody()
                } else {
                    formBody.toRequestBody()
                }
            )
            build()
        }

        return request.execute(client, log)
    }

    fun get(
        url: String,
        path: String,
        headers: Map<String, String>,
        queryParameters: Map<String, String> = emptyMap(),
    ): Response {
        val requestUrl = buildRequestUrl(url, path, queryParameters)

        val request = Request.Builder().run {
            url(requestUrl)
            headers.forEach {
                addHeader(it.key, it.value)
            }
            get()
            build()
        }

        return request.execute(client, log)
    }
}

internal fun buildRequestUrl(url: String, path: String, queryParameters: Map<String, String>): HttpUrl {
    val origin = url.toHttpUrl()
    require(origin.encodedPath == "/" && origin.query == null) {
        "The base URL must be a bare origin, with no path: $url"
    }
    return origin.newBuilder().run {
        addPathSegments(path)
        queryParameters.forEach {
            addQueryParameter(it.key, it.value)
        }
        build()
    }
}

private fun Map<String, String>.toRequestBody() =
    FormBody.Builder().run {
        forEach {
            add(it.key, it.value)
        }
        build()
    }

private fun Request.execute(client: Call.Factory, log: Logger): Response {
    log.event(
        TelemetryAttributes.HTTP_REQUEST_SENT,
        TelemetryAttributes.HTTP_REQUEST_METHOD to method,
        TelemetryAttributes.SERVER_ADDRESS to url.host,
        TelemetryAttributes.URL_PATH to url.encodedPath,
        level = EventLevel.DEBUG,
    )
    return client.newCall(this).execute()
}

package com.rafaelfo.labelfollower.integrations.httputils

import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.springframework.stereotype.Component

@Component
class RafaHttp {

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

        return request.execute()
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

        return request.execute()
    }
}

internal fun buildRequestUrl(url: String, path: String, queryParameters: Map<String, String>): HttpUrl {
    val (scheme, host) = url.split("://")
    return HttpUrl.Builder().run {
        scheme(scheme)
        host(host)
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

private fun Request.execute(): Response {
    println("RafaHttp: $this")
    return OkHttpClient().newCall(this).execute()
}

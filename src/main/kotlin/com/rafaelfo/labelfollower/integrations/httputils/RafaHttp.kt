package com.rafaelfo.labelfollower.integrations.httputils

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.springframework.stereotype.Component

@Component
class RafaHttp {

    fun post(
        url: String,
        body: Map<String, String>,
        headers: Map<String, String>,
    ): Response {
        val formBody = FormBody.Builder().run {
            body.forEach {
                add(it.key, it.value)
            }
            build()
        }

        val request = Request.Builder().run {
            url(url)
            headers.forEach {
                addHeader(it.key, it.value)
            }
            post(formBody)
            build()
        }

        return OkHttpClient().newCall(request).execute()
    }
}

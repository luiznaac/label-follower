package com.rafaelfo.labelfollower.integrations.httputils

import com.rafaelfo.labelfollower.usecases.telemetry.TelemetryAttributes
import okhttp3.Interceptor
import okhttp3.Response
import org.slf4j.MDC

/**
 * Propagates the inbound `request.id` on every outbound call from the shared client, so a
 * chain of services shares one id with no per-hop regeneration. `docs/observability.md` §3.
 */
class RequestIdInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestId = MDC.get(TelemetryAttributes.REQUEST_ID)
        val request = if (requestId == null) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header(TelemetryAttributes.REQUEST_ID_HEADER, requestId)
                .build()
        }
        return chain.proceed(request)
    }
}

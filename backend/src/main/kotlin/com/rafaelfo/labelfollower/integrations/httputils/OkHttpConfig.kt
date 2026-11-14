package com.rafaelfo.labelfollower.integrations.httputils

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.okhttp.v3_0.OkHttpTelemetry
import okhttp3.Call
import okhttp3.OkHttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * The one shared outbound client: the OkHttp instrumentation owns `http.client.request.duration`
 * and [RequestIdInterceptor] propagates the request id. `docs/observability.md` §5 (HTTP client).
 */
@Configuration
class OkHttpConfig(
    private val openTelemetry: OpenTelemetry,
) {

    @Bean
    fun outboundHttpClient(): Call.Factory = OkHttpTelemetry.create(openTelemetry)
        .createCallFactory(
            OkHttpClient.Builder()
                .addInterceptor(RequestIdInterceptor())
                .build(),
        )
}

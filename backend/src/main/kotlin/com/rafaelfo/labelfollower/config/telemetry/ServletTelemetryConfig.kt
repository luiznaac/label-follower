package com.rafaelfo.labelfollower.config.telemetry

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.spring.webmvc.v6_0.SpringWebMvcTelemetry
import jakarta.servlet.Filter
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Wires the two servlet surfaces: the official Spring MVC instrumentation filter (the
 * `http.server.request.duration` histogram) and the boundary resolver that hands the exception
 * to [RequestTelemetryFilter]. `docs/observability.md` §5 and §6.
 */
@Configuration
class ServletTelemetryConfig(
    private val openTelemetry: OpenTelemetry,
    private val boundaryTelemetry: BoundaryTelemetry,
) : WebMvcConfigurer {

    @Bean
    fun springWebMvcTelemetryFilter(): FilterRegistrationBean<Filter> =
        FilterRegistrationBean(SpringWebMvcTelemetry.create(openTelemetry).createServletFilter()).apply {
            order = Ordered.HIGHEST_PRECEDENCE + 1
        }

    override fun extendHandlerExceptionResolvers(resolvers: MutableList<HandlerExceptionResolver>) {
        resolvers.add(0, boundaryTelemetry)
    }
}

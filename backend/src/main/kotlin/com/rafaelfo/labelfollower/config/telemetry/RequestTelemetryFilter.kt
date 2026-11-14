package com.rafaelfo.labelfollower.config.telemetry

import com.rafaelfo.labelfollower.usecases.telemetry.EventLevel
import com.rafaelfo.labelfollower.usecases.telemetry.MetricCatalog
import com.rafaelfo.labelfollower.usecases.telemetry.TelemetryAttributes
import com.rafaelfo.labelfollower.usecases.telemetry.event
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.Meter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerMapping

private const val BAD_REQUEST = 400
private const val SERVER_ERROR = 500

/**
 * The single servlet correlation seam: it binds the request id before the chain (so framework,
 * usecase and gateway records all see it), echoes it, keeps the in-flight gauge honest and
 * emits the one boundary record per failed request. Successful requests stay out of the log
 * lane. `docs/observability.md` §3 (correlation) and §5 (HTTP server).
 */
// The correlation boundary runs ahead of routing and ahead of the framework instrumentation
// filter, which follows one rung behind so the request id is bound before anything else runs.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestTelemetryFilter(
    meter: Meter,
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(RequestTelemetryFilter::class.java)

    private val activeRequests = meter.upDownCounterBuilder(MetricCatalog.HTTP_SERVER_ACTIVE_REQUESTS)
        .setDescription("Number of server requests currently in flight")
        .setUnit(MetricCatalog.REQUESTS)
        .build()

    @Suppress("TooGenericExceptionCaught") // The boundary owns every servlet exception, by design.
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val requestId = RequestId.resolve(request.getHeader(TelemetryAttributes.REQUEST_ID_HEADER))
        MDC.put(TelemetryAttributes.REQUEST_ID, requestId)
        response.setHeader(TelemetryAttributes.REQUEST_ID_HEADER, requestId)
        val activeAttributes = activeAttributes(request)
        activeRequests.add(1, activeAttributes)
        var failure: Throwable? = null
        try {
            filterChain.doFilter(request, response)
        } catch (caughtFailure: Exception) {
            // The boundary owns the response: consume the unhandled failure into the container
            // error dispatch (Boot's error body) instead of rethrowing, which would make Tomcat
            // log a second ERROR for the same request.
            failure = caughtFailure
            if (!response.isCommitted) {
                response.sendError(SERVER_ERROR)
            }
        } finally {
            activeRequests.add(-1, activeAttributes)
            recordBoundary(request, response, requestId, failure)
            MDC.remove(TelemetryAttributes.REQUEST_ID)
        }
    }

    private fun recordBoundary(
        request: HttpServletRequest,
        response: HttpServletResponse,
        requestId: String,
        failure: Throwable?,
    ) {
        val status = response.status
        if (status < BAD_REQUEST) {
            return
        }
        val stored = request.getAttribute(BoundaryTelemetry.EXCEPTION_ATTRIBUTE) as? Throwable
        val exception = stored ?: failure?.let(::underlyingFailure)
        val level = if (status >= SERVER_ERROR) EventLevel.ERROR else EventLevel.WARN
        val attributes = requestAttributes(request, status, errorTypeOf(status, exception), requestId)
        val throwable = exception.takeIf { status >= SERVER_ERROR }
        log.event(
            TelemetryAttributes.HTTP_REQUEST_FAILED,
            *attributes.toTypedArray(),
            level = level,
            throwable = throwable
        )
    }

    private fun underlyingFailure(failure: Throwable): Throwable =
        if (failure is ServletException && failure.cause != null) failure.cause!! else failure

    // A 5xx normally carries the failure's class name; a controller that sets 500 directly
    // has no exception, so the status string is the only bounded value left.
    private fun errorTypeOf(status: Int, exception: Throwable?): String =
        if (status >= SERVER_ERROR) {
            exception?.let { it::class.java.simpleName } ?: status.toString()
        } else {
            TelemetryAttributes.errorCodeFor(status)
        }

    private fun requestAttributes(
        request: HttpServletRequest,
        status: Int,
        errorType: String,
        requestId: String,
    ): List<Pair<String, Any?>> = buildList {
        add(TelemetryAttributes.HTTP_REQUEST_METHOD to request.method)
        val route = controllerRouteOf(request)
        if (route == null) {
            add(TelemetryAttributes.URL_PATH to request.requestURI)
        } else {
            add(TelemetryAttributes.HTTP_ROUTE to route)
        }
        add(TelemetryAttributes.HTTP_RESPONSE_STATUS_CODE to status)
        add(TelemetryAttributes.ERROR_TYPE to errorType)
        add(TelemetryAttributes.REQUEST_ID to requestId)
    }

    /**
     * Only a controller's route pattern is a `http.route`; the static-resource handler also
     * matches the catch-all pattern, and the standard requires an unmatched 404 to leave the attribute absent.
     */
    private fun controllerRouteOf(request: HttpServletRequest): String? {
        if (request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE) !is HandlerMethod) {
            return null
        }
        return request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE) as? String
    }

    private fun activeAttributes(request: HttpServletRequest) = Attributes.of(
        AttributeKey.stringKey(TelemetryAttributes.HTTP_REQUEST_METHOD),
        request.method,
        AttributeKey.stringKey(TelemetryAttributes.URL_SCHEME),
        request.scheme,
    )
}

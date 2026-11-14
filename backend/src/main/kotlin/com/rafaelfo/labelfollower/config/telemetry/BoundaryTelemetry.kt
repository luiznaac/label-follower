package com.rafaelfo.labelfollower.config.telemetry

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.ModelAndView

/**
 * The highest-precedence exception resolver. It stores the failure for
 * [RequestTelemetryFilter] and returns `null`, so the standard resolvers still own the
 * response and a mappable 4xx status survives. A failure no resolver handles reaches the
 * filter, which consumes it into the container error dispatch and emits the single boundary
 * record. `docs/observability.md` §3 (Error telemetry).
 */
@Component
class BoundaryTelemetry : HandlerExceptionResolver {

    override fun resolveException(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any?,
        ex: Exception,
    ): ModelAndView? {
        request.setAttribute(EXCEPTION_ATTRIBUTE, ex)
        return null
    }

    companion object {
        const val EXCEPTION_ATTRIBUTE = "com.rafaelfo.labelfollower.telemetry.boundaryFailure"
    }
}

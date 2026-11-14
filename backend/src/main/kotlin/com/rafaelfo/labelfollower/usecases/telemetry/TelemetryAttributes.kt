package com.rafaelfo.labelfollower.usecases.telemetry

/**
 * Attribute, field and error-code names shared by the log record, the metrics catalog and the
 * request correlation contract in `docs/observability.md`. Values are the semconv spelling
 * verbatim.
 */
object TelemetryAttributes {

    const val EVENT_NAME = "event.name"
    const val HTTP_REQUEST_FAILED = "http.request.failed"
    const val HTTP_REQUEST_SENT = "http.request.sent"
    const val TELEMETRY_EXPORT_FAILED = "telemetry.export.failed"
    const val AUTH_TOKEN_REFRESHED = "auth.token.refreshed"
    const val LABELS_CONSOLIDATED = "labels.consolidated"
    const val HEALTH_CHECK_FAILED = "health.check.failed"
    const val HEALTH_CHECK_RECOVERED = "health.check.recovered"
    const val REQUEST_ID = "request.id"
    const val REQUEST_ID_HEADER = "X-Request-ID"

    const val HTTP_REQUEST_METHOD = "http.request.method"
    const val HTTP_ROUTE = "http.route"
    const val URL_PATH = "url.path"
    const val URL_SCHEME = "url.scheme"
    const val HTTP_RESPONSE_STATUS_CODE = "http.response.status_code"
    const val NETWORK_PROTOCOL_VERSION = "network.protocol.version"
    const val SERVER_ADDRESS = "server.address"
    const val SERVER_PORT = "server.port"
    const val ERROR_TYPE = "error.type"
    const val HEALTH_COMPONENT = "health.component"

    const val RESOURCE_NOT_FOUND = "resource_not_found"
    const val INVALID_ARGUMENT = "invalid_argument"
    const val UNAUTHENTICATED = "unauthenticated"
    const val PERMISSION_DENIED = "permission_denied"
    const val CONFLICT = "conflict"
    const val DOMAIN_ERROR = "domain_error"

    const val SERVICE_NAME = "service.name"
    const val SERVICE_NAMESPACE = "service.namespace"
    const val SERVICE_VERSION = "service.version"
    const val DEPLOYMENT_ENVIRONMENT_NAME = "deployment.environment.name"
    const val SERVICE_INSTANCE_ID = "service.instance.id"
    const val HOST_NAME = "host.name"

    val INBOUND_REQUEST_ID = Regex("^[A-Za-z0-9._-]{8,128}$")

    /** The stable, bounded `error.type` a 4xx transport status maps to. */
    fun errorCodeFor(status: Int): String = when (status) {
        STATUS_BAD_REQUEST, STATUS_UNPROCESSABLE_ENTITY -> INVALID_ARGUMENT
        STATUS_UNAUTHORIZED -> UNAUTHENTICATED
        STATUS_FORBIDDEN -> PERMISSION_DENIED
        STATUS_NOT_FOUND -> RESOURCE_NOT_FOUND
        STATUS_CONFLICT -> CONFLICT
        else -> DOMAIN_ERROR
    }

    private const val STATUS_BAD_REQUEST = 400
    private const val STATUS_UNAUTHORIZED = 401
    private const val STATUS_FORBIDDEN = 403
    private const val STATUS_NOT_FOUND = 404
    private const val STATUS_CONFLICT = 409
    private const val STATUS_UNPROCESSABLE_ENTITY = 422
}

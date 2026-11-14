package com.rafaelfo.labelfollower.config.telemetry

import com.rafaelfo.labelfollower.usecases.telemetry.TelemetryAttributes
import java.util.UUID

/**
 * Resolves the request id: a valid inbound header is kept, anything else is replaced with a
 * canonical lowercase UUIDv4. `docs/observability.md` §3 is the contract.
 */
object RequestId {

    val GENERATED_REQUEST_ID = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")

    fun resolve(inbound: String?): String =
        inbound?.takeIf(TelemetryAttributes.INBOUND_REQUEST_ID::matches) ?: UUID.randomUUID().toString()
}

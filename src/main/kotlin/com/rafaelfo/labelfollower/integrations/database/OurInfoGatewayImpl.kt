package com.rafaelfo.labelfollower.integrations.database

import com.rafaelfo.labelfollower.models.Label
import com.rafaelfo.labelfollower.models.Track
import com.rafaelfo.labelfollower.usecases.OurInfoGateway
import org.springframework.stereotype.Component

@Component
class OurInfoGatewayImpl : OurInfoGateway {

    override fun getTracksFrom(label: Label): Set<Track> {
        return emptySet()
    }
}

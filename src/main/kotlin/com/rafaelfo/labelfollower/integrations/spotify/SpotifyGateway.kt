package com.rafaelfo.labelfollower.integrations.spotify

import com.rafaelfo.labelfollower.models.Label
import com.rafaelfo.labelfollower.models.Track
import com.rafaelfo.labelfollower.usecases.ExternalInfoGateway
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import org.springframework.stereotype.Component


@Component
class SpotifyGateway(
    private val spotifyTrackGateway: SpotifyTrackGateway,
    private val spotifyAlbumGateway: SpotifyAlbumGateway,
    private val spotifyLabelGateway: SpotifyLabelGateway,
) : ExternalInfoGateway {

    override fun getLabel(isrc: String): Label {
        return spotifyTrackGateway.findTrackBy(isrc).run {
            spotifyAlbumGateway.findAlbumBy(this).toLabel()
        }
    }

    override fun findTrackBy(isrc: String): Track {
        return spotifyTrackGateway.findTrackBy(isrc).toTrack()
    }

    override fun getTracksFrom(label: Label): Set<Track> {
        return spotifyLabelGateway.findAlbumsBy(label)
            .run { this.map { it.id }.toSet() }
            .run { spotifyAlbumGateway.findAlbumsById(this) }
            .filter { label.matches(it.toLabel()) }
            .filter { bestAfter.isAfter(it.release_date) }
            .flatMap { it.tracks!!.items }
            .run { this.map { it.id }.toSet() }
            .run { spotifyTrackGateway.findTracksById(this) }
            .map { it.toTrack() }
            .toSet()
    }

    companion object {
        private val bestAfter = "2021-11-01".parseUTC()
    }
}

private fun Instant.isAfter(releaseDate: String): Boolean {
    return releaseDate.parseUTC().isAfter(this)
}

private fun String.parseUTC() : Instant {
    val localDate = LocalDate.from(
        DateTimeFormatter.ISO_LOCAL_DATE.parse(this)
    )
    val zonedDateTime = ZonedDateTime.of(localDate.atTime(0, 0), ZoneOffset.UTC)
    return Instant.from(zonedDateTime)
}

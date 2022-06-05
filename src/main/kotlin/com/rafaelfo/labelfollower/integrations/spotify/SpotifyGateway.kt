package com.rafaelfo.labelfollower.integrations.spotify

import com.rafaelfo.labelfollower.integrations.spotify.models.SpotifyAlbum
import com.rafaelfo.labelfollower.models.Label
import com.rafaelfo.labelfollower.models.Track
import com.rafaelfo.labelfollower.usecases.ExternalInfoGateway
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
        val labelAlbums = spotifyLabelGateway.findAlbumsBy(label)
        val fullAlbums = spotifyAlbumGateway.findAlbumsBy(labelAlbums)
        println(fullAlbums.filter { it.toLabel() == label }.flatMap { it.tracks!!.items })
        TODO()
    }
}

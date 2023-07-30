package com.rafaelfo.labelfollower.integrations.spotify

import com.rafaelfo.labelfollower.integrations.httputils.RafaHttp
import com.rafaelfo.labelfollower.integrations.httputils.parsedBody
import com.rafaelfo.labelfollower.models.Label
import com.rafaelfo.labelfollower.models.Track
import com.rafaelfo.labelfollower.usecases.UserInfoGateway
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class SpotifyUserPlaylistGateway(
    private val spotifyConfig: SpotifyConfig,
    private val rafaHttp: RafaHttp,
) : UserInfoGateway {

    override fun createPlaylistWith(label: Label, tracks: Set<Track>, userToken: String) {
        val playlistId = rafaHttp.post(
            url = "${spotifyConfig.apiUri}/v1/users/12183121385/playlists",
            headers = mapOf("Authorization" to "Bearer $userToken"),
            body = mapOf(
                "name" to "${Instant.now()}-${label.name}",
            ),
        ).parsedBody<SpotifyCreatePlaylistResponse>().id

        tracks.chunked(size = 5)
            .forEach { chunkedTracks ->
                rafaHttp.post(
                    url = "${spotifyConfig.apiUri}/v1/playlists/$playlistId/tracks",
                    headers = mapOf("Authorization" to "Bearer $userToken"),
                    body = mapOf(
                        "uris" to chunkedTracks.map { "spotify:track:${it.spotifyId}" },
                    ),
                )
            }
    }
}

private data class SpotifyCreatePlaylistResponse(val id: String)

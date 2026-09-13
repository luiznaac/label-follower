package com.rafaelfo.labelfollower.integrations.spotify

import com.rafaelfo.labelfollower.integrations.spotify.models.SpotifyAlbum
import com.rafaelfo.labelfollower.integrations.spotify.models.SpotifyArtist
import com.rafaelfo.labelfollower.integrations.spotify.models.SpotifyCopyrights
import com.rafaelfo.labelfollower.integrations.spotify.models.SpotifyExternalIds
import com.rafaelfo.labelfollower.integrations.spotify.models.SpotifyTrack
import com.rafaelfo.labelfollower.integrations.spotify.responses.TrackItems
import com.rafaelfo.labelfollower.models.Label
import com.rafaelfo.labelfollower.models.Track
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class SpotifyGatewayTest : StringSpec({

    val spotifyTrackGateway = mockk<SpotifyTrackGateway>()
    val spotifyAlbumGateway = mockk<SpotifyAlbumGateway>()
    val spotifyLabelGateway = mockk<SpotifyLabelGateway>()
    val gateway = SpotifyGateway(spotifyTrackGateway, spotifyAlbumGateway, spotifyLabelGateway)
    val requestedLabel = Label(name = "Label 1", copyrights = setOf("Label 1"))

    afterEach {
        clearMocks(spotifyTrackGateway, spotifyAlbumGateway, spotifyLabelGateway)
    }

    "should include tracks from an album with year precision release date" {
        val track = spotifyTrack(id = "track-1")
        val album = spotifyAlbum(id = "album-1", releaseDate = "2023", tracks = listOf(track))

        every { spotifyLabelGateway.findAlbumsBy(requestedLabel) } returns setOf(
            spotifyAlbum(id = "album-1", releaseDate = "2023"),
        )
        every { spotifyAlbumGateway.findAlbumsById(setOf("album-1")) } returns setOf(album)
        every { spotifyTrackGateway.findTracksById(setOf("track-1")) } returns setOf(track)

        gateway.getTracksFrom(requestedLabel) shouldBe setOf(track.toTrack())
    }

    "should include tracks from an album with year-month precision release date" {
        val track = spotifyTrack(id = "track-2")
        val album = spotifyAlbum(id = "album-2", releaseDate = "2023-05", tracks = listOf(track))

        every { spotifyLabelGateway.findAlbumsBy(requestedLabel) } returns setOf(
            spotifyAlbum(id = "album-2", releaseDate = "2023-05"),
        )
        every { spotifyAlbumGateway.findAlbumsById(setOf("album-2")) } returns setOf(album)
        every { spotifyTrackGateway.findTracksById(setOf("track-2")) } returns setOf(track)

        gateway.getTracksFrom(requestedLabel) shouldBe setOf(track.toTrack())
    }

    "should include only releases strictly after the cutoff date" {
        val track3 = spotifyTrack(id = "track-3")
        val track4 = spotifyTrack(id = "track-4")
        val track5 = spotifyTrack(id = "track-5")
        val track6 = spotifyTrack(id = "track-6")
        val albums = setOf(
            spotifyAlbum(
                id = "album-1",
                releaseDate = "2021-10-31",
                tracks = listOf(spotifyTrack(id = "track-1")),
            ),
            spotifyAlbum(
                id = "album-2",
                releaseDate = "2021-11-01",
                tracks = listOf(spotifyTrack(id = "track-2")),
            ),
            spotifyAlbum(id = "album-3", releaseDate = "2021-11-02", tracks = listOf(track3)),
            spotifyAlbum(id = "album-4", releaseDate = "2021", tracks = listOf(track4)),
            spotifyAlbum(id = "album-5", releaseDate = "2021-11", tracks = listOf(track5)),
            spotifyAlbum(id = "album-6", releaseDate = "2021-12", tracks = listOf(track6)),
            spotifyAlbum(
                id = "album-7",
                releaseDate = "2021-10",
                tracks = listOf(spotifyTrack(id = "track-7")),
            ),
        )

        every { spotifyLabelGateway.findAlbumsBy(requestedLabel) } returns setOf(
            spotifyAlbum(id = "album-1", releaseDate = "2021-10-31"),
            spotifyAlbum(id = "album-2", releaseDate = "2021-11-01"),
            spotifyAlbum(id = "album-3", releaseDate = "2021-11-02"),
            spotifyAlbum(id = "album-4", releaseDate = "2021"),
            spotifyAlbum(id = "album-5", releaseDate = "2021-11"),
            spotifyAlbum(id = "album-6", releaseDate = "2021-12"),
            spotifyAlbum(id = "album-7", releaseDate = "2021-10"),
        )
        every {
            spotifyAlbumGateway.findAlbumsById(
                setOf("album-1", "album-2", "album-3", "album-4", "album-5", "album-6", "album-7")
            )
        } returns albums
        every {
            spotifyTrackGateway.findTracksById(setOf("track-3", "track-4", "track-5", "track-6"))
        } returns setOf(track3, track4, track5, track6)

        gateway.getTracksFrom(requestedLabel) shouldBe
            setOf(track3.toTrack(), track4.toTrack(), track5.toTrack(), track6.toTrack())

        verify(exactly = 1) {
            spotifyTrackGateway.findTracksById(setOf("track-3", "track-4", "track-5", "track-6"))
        }
    }

    "should drop albums whose label does not match the requested label" {
        val requested = Label(name = "Label 1", copyrights = setOf("Label 1"))
        val matchingTrack = spotifyTrack(id = "track-1")
        val matching = spotifyAlbum(
            id = "album-1",
            releaseDate = "2023-06-01",
            tracks = listOf(matchingTrack),
        )
        val otherName = spotifyAlbum(
            id = "album-2",
            releaseDate = "2023-06-01",
            label = "Label 2",
            tracks = listOf(spotifyTrack(id = "track-2")),
        )
        val otherCopyright = spotifyAlbum(
            id = "album-3",
            releaseDate = "2023-06-01",
            copyrights = listOf("2023 Somewhere Else"),
            tracks = listOf(spotifyTrack(id = "track-3")),
        )

        every { spotifyLabelGateway.findAlbumsBy(requested) } returns setOf(
            spotifyAlbum(id = "album-1", releaseDate = "2023-06-01"),
            spotifyAlbum(id = "album-2", releaseDate = "2023-06-01"),
            spotifyAlbum(id = "album-3", releaseDate = "2023-06-01"),
        )
        every {
            spotifyAlbumGateway.findAlbumsById(setOf("album-1", "album-2", "album-3"))
        } returns setOf(matching, otherName, otherCopyright)
        every { spotifyTrackGateway.findTracksById(setOf("track-1")) } returns setOf(matchingTrack)

        gateway.getTracksFrom(requested) shouldBe setOf(matchingTrack.toTrack())

        verify(exactly = 1) { spotifyTrackGateway.findTracksById(setOf("track-1")) }
    }

    "should find tracks once with deduplicated ids and collapse duplicated tracks" {
        val track1 = spotifyTrack(id = "track-1")
        val track2 = spotifyTrack(id = "track-2")
        val album1 = spotifyAlbum(
            id = "album-1",
            releaseDate = "2023-06-01",
            tracks = listOf(track1, track2),
        )
        val album2 = spotifyAlbum(id = "album-2", releaseDate = "2023-06-01", tracks = listOf(track1))

        every { spotifyLabelGateway.findAlbumsBy(requestedLabel) } returns setOf(
            spotifyAlbum(id = "album-1", releaseDate = "2023-06-01"),
            spotifyAlbum(id = "album-2", releaseDate = "2023-06-01"),
        )
        every { spotifyAlbumGateway.findAlbumsById(setOf("album-1", "album-2")) } returns setOf(
            album1,
            album2,
        )
        every {
            spotifyTrackGateway.findTracksById(setOf("track-1", "track-2"))
        } returns setOf(track1, track2)

        gateway.getTracksFrom(requestedLabel) shouldBe setOf(track1.toTrack(), track2.toTrack())

        verify(exactly = 1) { spotifyTrackGateway.findTracksById(setOf("track-1", "track-2")) }
    }

    "should get label by composing track and album lookups" {
        val isrc = "ISRC-1"
        val track = spotifyTrack(id = "track-1", isrc = isrc)
        val album = spotifyAlbum(
            id = "album-1",
            releaseDate = "2023-06-01",
            label = "Awesome Records",
            copyrights = listOf("2023 Awesome"),
        )

        every { spotifyTrackGateway.findTrackBy(isrc) } returns track
        every { spotifyAlbumGateway.findAlbumBy(track) } returns album

        gateway.getLabel(isrc) shouldBe Label(name = "Awesome", copyrights = setOf("Awesome"))

        verify(exactly = 1) {
            spotifyTrackGateway.findTrackBy(isrc)
            spotifyAlbumGateway.findAlbumBy(track)
        }
    }

    "should map found track through toTrack" {
        val isrc = "ISRC-1"
        val track = spotifyTrack(id = "track-1", name = "Track 1", isrc = isrc)

        every { spotifyTrackGateway.findTrackBy(isrc) } returns track

        gateway.findTrackBy(isrc) shouldBe Track(name = "Track 1", isrc = isrc, spotifyId = "track-1")

        verify(exactly = 1) { spotifyTrackGateway.findTrackBy(isrc) }
    }
})

private fun spotifyTrack(
    id: String,
    name: String = "Track $id",
    isrc: String = "ISRC-$id",
) = SpotifyTrack(
    id = id,
    name = name,
    artists = setOf(SpotifyArtist(name = "Artist $id")),
    album = null,
    external_ids = SpotifyExternalIds(isrc = isrc),
)

private fun spotifyAlbum(
    id: String,
    releaseDate: String,
    label: String = "Label 1",
    copyrights: List<String> = listOf("2023 Label 1"),
    tracks: List<SpotifyTrack> = emptyList(),
) = SpotifyAlbum(
    id = id,
    name = "Album $id",
    copyrights = copyrights.map { SpotifyCopyrights(text = it) },
    tracks = TrackItems(items = tracks, limit = null, offset = null, total = null),
    label = label,
    release_date = releaseDate,
)

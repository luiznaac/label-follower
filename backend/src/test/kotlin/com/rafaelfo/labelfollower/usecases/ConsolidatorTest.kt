package com.rafaelfo.labelfollower.usecases

import com.rafaelfo.labelfollower.models.Label
import com.rafaelfo.labelfollower.models.Track
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder

// Consolidator runs against a real LabelIntrospector and mocked ports, so these tests pin the
// order in which the two collaborate — the part that used to lose tracks (B2 in
// docs/bugs-e-melhorias.md): new tracks were recorded as known *before* their playlist existed.
class ConsolidatorTest : StringSpec({

    val external = mockk<ExternalInfoGateway>()
    val ours = mockk<OurInfoGateway>()
    val playlists = mockk<UserInfoGateway>()
    val consolidator = Consolidator(ours, LabelIntrospector(external, ours), playlists)

    val labelA = Label(name = "Label A", copyrights = setOf("Label A"))
    val labelB = Label(name = "Label B", copyrights = setOf("Label B"))
    val known = Track(name = "Known", isrc = "AAA000000001", spotifyId = "k1")
    val newA = Track(name = "New A", isrc = "AAA000000002", spotifyId = "a2")
    val newB = Track(name = "New B", isrc = "BBB000000001", spotifyId = "b1")

    beforeEach {
        clearMocks(external, ours, playlists)
        every { ours.saveTracks(any(), any()) } just Runs
    }

    "new tracks are only marked as known after the label's playlist exists" {
        every { ours.getLabels() } returns setOf(labelA)
        every { external.getTracksFrom(labelA) } returns setOf(known, newA)
        every { ours.getTracksFrom(labelA) } returns setOf(known)
        every { playlists.createPlaylistWith(labelA, setOf(newA)) } just Runs

        consolidator.introspectAllLabelsAndNotify()

        verifyOrder {
            playlists.createPlaylistWith(labelA, setOf(newA))
            ours.saveTracks(setOf(newA), labelA)
        }
    }

    "tracks stay new when their playlist can't be created, so the next run picks them up" {
        every { ours.getLabels() } returns setOf(labelA)
        every { external.getTracksFrom(labelA) } returns setOf(newA)
        every { ours.getTracksFrom(labelA) } returns emptySet()
        every { playlists.createPlaylistWith(any(), any()) } throws
            IllegalStateException("No Spotify account connected")

        val error = shouldThrow<ConsolidationFailedException> { consolidator.introspectAllLabelsAndNotify() }

        verify(exactly = 0) { ours.saveTracks(match { it.isNotEmpty() }, any()) }
        error.report.failures.map { it.label } shouldBe listOf(labelA)
        error.message!! shouldContain "No Spotify account connected"
    }

    "a label that fails doesn't stop the others" {
        every { ours.getLabels() } returns setOf(labelA, labelB)
        every { external.getTracksFrom(labelA) } returns setOf(newA)
        every { external.getTracksFrom(labelB) } returns setOf(newB)
        every { ours.getTracksFrom(any()) } returns emptySet()
        every { playlists.createPlaylistWith(labelA, any()) } throws IllegalStateException("Spotify said no")
        every { playlists.createPlaylistWith(labelB, any()) } just Runs

        val error = shouldThrow<ConsolidationFailedException> { consolidator.introspectAllLabelsAndNotify() }

        verify { playlists.createPlaylistWith(labelB, setOf(newB)) }
        verify { ours.saveTracks(setOf(newB), labelB) }
        verify(exactly = 0) { ours.saveTracks(setOf(newA), labelA) }
        error.report.results.map { it::class } shouldBe listOf(
            LabelConsolidation.Failed::class,
            LabelConsolidation.PlaylistCreated::class,
        )
    }

    "a label that can't even be read from Spotify doesn't stop the others either" {
        every { ours.getLabels() } returns setOf(labelA, labelB)
        every { external.getTracksFrom(labelA) } throws IllegalStateException("HTTP 502")
        every { external.getTracksFrom(labelB) } returns setOf(newB)
        every { ours.getTracksFrom(any()) } returns emptySet()
        every { playlists.createPlaylistWith(labelB, any()) } just Runs

        shouldThrow<ConsolidationFailedException> { consolidator.introspectAllLabelsAndNotify() }

        verify { ours.saveTracks(setOf(newB), labelB) }
    }

    "labels without new tracks get no playlist and record nothing" {
        every { ours.getLabels() } returns setOf(labelA)
        every { external.getTracksFrom(labelA) } returns setOf(known)
        every { ours.getTracksFrom(labelA) } returns setOf(known)

        val report = consolidator.introspectAllLabelsAndNotify()

        verify(exactly = 0) { playlists.createPlaylistWith(any(), any()) }
        verify(exactly = 0) { ours.saveTracks(any(), any()) }
        report.results shouldBe listOf(LabelConsolidation.NothingNew(labelA))
    }
})

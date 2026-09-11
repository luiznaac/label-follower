package com.rafaelfo.labelfollower.usecases

import com.rafaelfo.labelfollower.models.Label
import com.rafaelfo.labelfollower.models.Track
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk

class LabelIntrospectorTest : StringSpec({

    val externalInfoGateway = mockk<ExternalInfoGateway>()
    val ourInfoGateway = mockk<OurInfoGateway>()
    val introspector = LabelIntrospector(externalInfoGateway, ourInfoGateway)

    afterEach {
        clearMocks(externalInfoGateway, ourInfoGateway)
    }

    "should correctly get label from track and search for tracks" {
        val label = Label(name = "Label 1", copyrights = setOf("Copyrigth 1"))
        val track1 = Track(name = "Track 1", isrc = "AABB123", spotifyId = "1")
        val track2 = Track(name = "Track 2", isrc = "CCDD456", spotifyId = "2")

        coEvery { externalInfoGateway.getLabel(any()) } returns label
        coEvery { externalInfoGateway.getTracksFrom(any()) } returns setOf(track1, track2)
        coEvery { ourInfoGateway.getLabelBy(any()) } returns null

        introspector.introspectFrom(track1) shouldBe setOf(track1, track2)

        coVerify(exactly = 1) {
            externalInfoGateway.getLabel(track1.isrc)
            externalInfoGateway.getTracksFrom(label)
        }
    }

    "should find new tracks from label without recording them" {
        val label = Label(name = "Label 1", copyrights = setOf("Copyrigth 1"))
        val alreadyFoundTrack1 = Track(name = "Track 1", isrc = "AABB123", spotifyId = "1")
        val alreadyFoundTrack2 = Track(name = "Track 2", isrc = "ZZYY543", spotifyId = "2")
        val newTrack1 = Track(name = "Track 3", isrc = "CCDD456", spotifyId = "3")
        val newTrack2 = Track(name = "Track 4", isrc = "GGJJ987", spotifyId = "4")

        coEvery { externalInfoGateway.getTracksFrom(any()) } returns setOf(
            alreadyFoundTrack1,
            alreadyFoundTrack2,
            newTrack1,
            newTrack2
        )
        coEvery { ourInfoGateway.getTracksFrom(any()) } returns setOf(alreadyFoundTrack1, alreadyFoundTrack2)

        introspector.findNewTracksFrom(label) shouldBe setOf(newTrack1, newTrack2)

        coVerify(exactly = 1) {
            externalInfoGateway.getTracksFrom(label)
            ourInfoGateway.getTracksFrom(label)
        }
        coVerify(exactly = 0) { ourInfoGateway.saveTracks(any(), any()) }
    }

    "discovering from a track records the new tracks under the track's label" {
        val label = Label(name = "Label 1", copyrights = setOf("Copyrigth 1"))
        val seed = Track(name = "Seed", isrc = "AABB123", spotifyId = "1")
        val newTrack = Track(name = "New", isrc = "CCDD456", spotifyId = "3")

        coEvery { externalInfoGateway.getLabel(seed.isrc) } returns label
        coEvery { ourInfoGateway.getLabelBy(label.name) } returns null
        coEvery { externalInfoGateway.getTracksFrom(label) } returns setOf(seed, newTrack)
        coEvery { ourInfoGateway.getTracksFrom(label) } returns setOf(seed)
        coEvery { ourInfoGateway.saveTracks(any(), any()) } just Runs

        introspector.discoverNewTracksFrom(seed) shouldBe setOf(newTrack)

        coVerify(exactly = 1) { ourInfoGateway.saveTracks(setOf(newTrack), label) }
    }
})

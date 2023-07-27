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
        val track1 = Track(name = "Track 1", isrc = "AABB123")
        val track2 = Track(name = "Track 2", isrc = "CCDD456")

        coEvery { externalInfoGateway.getLabel(any()) } returns label
        coEvery { externalInfoGateway.getTracksFrom(any()) } returns setOf(track1, track2)
        coEvery { ourInfoGateway.getLabelBy(any()) } returns null

        introspector.introspectFrom(track1) shouldBe setOf(track1, track2)

        coVerify(exactly = 1) {
            externalInfoGateway.getLabel(track1.isrc)
            externalInfoGateway.getTracksFrom(label)
        }
    }

    "should correctly get new tracks from label" {
        val label = Label(name = "Label 1", copyrights = setOf("Copyrigth 1"))
        val alreadyFoundTrack1 = Track(name = "Track 1", isrc = "AABB123")
        val alreadyFoundTrack2 = Track(name = "Track 2", isrc = "ZZYY543")
        val newTrack1 = Track(name = "Track 3", isrc = "CCDD456")
        val newTrack2 = Track(name = "Track 4", isrc = "GGJJ987")

        coEvery { externalInfoGateway.getTracksFrom(any()) } returns setOf(
            alreadyFoundTrack1, alreadyFoundTrack2, newTrack1, newTrack2
        )
        coEvery { ourInfoGateway.getTracksFrom(any()) } returns setOf(alreadyFoundTrack1, alreadyFoundTrack2)
        coEvery { ourInfoGateway.saveTracks(any(), any()) } just Runs

        introspector.discoverNewTracksFrom(label) shouldBe setOf(newTrack1, newTrack2)

        coVerify(exactly = 1) {
            externalInfoGateway.getTracksFrom(label)
            ourInfoGateway.getTracksFrom(label)
            ourInfoGateway.saveTracks(setOf(newTrack1, newTrack2), label)
        }
    }
})

package com.rafaelfo.labelfollower.usecases

import com.rafaelfo.labelfollower.models.Track
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk

class TrackFinderTest : StringSpec({

    val trackGateway = mockk<ExternalInfoGateway>()
    val finder = TrackFinder(trackGateway)

    "should correctly find track by isrc" {
        val isrc = "AABB123"
        val track = Track(name = "Track 1", isrc = isrc)
        coEvery { trackGateway.findTrackBy(any()) } returns track

        finder.findBy(isrc) shouldBe track

        coVerify(exactly = 1) {
            trackGateway.findTrackBy(isrc)
        }
    }
})

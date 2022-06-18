package com.rafaelfo.labelfollower.usecases

import com.rafaelfo.labelfollower.models.Label
import com.rafaelfo.labelfollower.models.Track
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk

class LabelIntrospectorTest : StringSpec({

    val externalInfoGateway = mockk<ExternalInfoGateway>()
    val introspector = LabelIntrospector(externalInfoGateway)

    "should correctly get label from track and search for tracks" {
        val label = Label(name = "Label 1", copyrights = setOf("Copyrigth 1"))
        val track1 = Track(name = "Track 1", artists = emptySet(), isrc = "AABB123")
        val track2 = Track(name = "Track 2", artists = emptySet(), isrc = "CCDD456")

        coEvery { externalInfoGateway.getLabel(any()) } returns label
        coEvery { externalInfoGateway.getTracksFrom(any()) } returns setOf(track1, track2)

        introspector.introspectFrom(track1) shouldBe setOf(track1, track2)

        coVerify(exactly = 1) {
            externalInfoGateway.getLabel(track1.isrc)
            externalInfoGateway.getTracksFrom(label)
        }
    }
})

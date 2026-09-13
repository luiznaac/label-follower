package com.rafaelfo.labelfollower.models

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class LabelTest : StringSpec({

    "should not match labels with different names even when copyrights overlap" {
        val label = Label(name = "Label 1", copyrights = setOf("2021 Label 1"))
        val other = Label(name = "Label 2", copyrights = setOf("2021 Label 1"))

        label.matches(other) shouldBe false
    }

    "should match labels with equal names and overlapping copyrights" {
        val label = Label(name = "Label 1", copyrights = setOf("2021 Label 1"))
        val other = Label(name = "Label 1", copyrights = setOf("label 1"))

        label.matches(other) shouldBe true
    }

    "should match labels when one copyright contains the other in either direction" {
        val label = Label(name = "Label 1", copyrights = setOf("2021 Label 1"))
        val other = Label(name = "Label 1", copyrights = setOf("2021 LABEL 1 MUSIC"))

        label.matches(other) shouldBe true
        other.matches(label) shouldBe true
    }

    "should match labels when only one of multiple copyrights overlaps" {
        val label = Label(name = "Label 1", copyrights = setOf("Something Else", "Label 1"))
        val other = Label(name = "Label 1", copyrights = setOf("Unrelated", "label 1"))

        label.matches(other) shouldBe true
    }

    "should not match labels with equal names and disjoint copyrights" {
        val label = Label(name = "Label 1", copyrights = setOf("Label 1"))
        val other = Label(name = "Label 1", copyrights = setOf("Label 2"))

        label.matches(other) shouldBe false
    }

    "should match labels with equal names when this label has no copyrights" {
        val label = Label(name = "Label 1", copyrights = emptySet())
        val other = Label(name = "Label 1", copyrights = setOf("Label 1"))

        label.matches(other) shouldBe true
    }

    "should match labels with equal names when the other label has no copyrights" {
        val label = Label(name = "Label 1", copyrights = setOf("Label 1"))
        val other = Label(name = "Label 1", copyrights = emptySet())

        label.matches(other) shouldBe true
    }

    "should match labels with equal names when both have no copyrights" {
        val label = Label(name = "Label 1", copyrights = emptySet())
        val other = Label(name = "Label 1", copyrights = emptySet())

        label.matches(other) shouldBe true
    }

    "should not match labels with different names when both have no copyrights" {
        val label = Label(name = "Label 1", copyrights = emptySet())
        val other = Label(name = "Label 2", copyrights = emptySet())

        label.matches(other) shouldBe false
    }
})

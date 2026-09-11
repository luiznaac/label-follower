package com.rafaelfo.labelfollower.integrations.database

import com.rafaelfo.labelfollower.config.migration.migrate
import com.rafaelfo.labelfollower.models.Label
import com.rafaelfo.labelfollower.models.Track
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.testcontainers.containers.MySQLContainer
import java.time.Clock

// OurInfoGatewayImpl against a real, migrated MySQL. Needs a Docker daemon (like MigrationSchemaTest).
class OurInfoGatewayImplTest : StringSpec({

    val mysql = MySQLContainer("mysql:9.4.0")
        .withDatabaseName("labelfollower")
        // Same override as MigrationSchemaTest — see mysql-test-conf/my.cnf.
        .withConfigurationOverride("mysql-test-conf")
    lateinit var database: Database
    val gateway = OurInfoGatewayImpl(Clock.systemUTC())

    val label = Label(name = "This Never Happened", copyrights = setOf("This Never Happened"))
    val track1 = Track(name = "Reviver - TEED Remix", isrc = "GBEWA2205680", spotifyId = "0LAOGIAFvEnMnzrO5oaAb5")
    val track2 = Track(name = "Another", isrc = "GBEWA2200001", spotifyId = "sp-2")

    beforeSpec {
        mysql.start()
        migrate(
            host = mysql.host,
            port = mysql.getMappedPort(MySQLContainer.MYSQL_PORT).toString(),
            user = mysql.username,
            password = mysql.password,
        )
        database = Database.connect(
            url = mysql.jdbcUrl,
            driver = "com.mysql.cj.jdbc.Driver",
            user = mysql.username,
            password = mysql.password,
        )
    }

    afterSpec {
        mysql.stop()
    }

    beforeEach {
        transaction(database) {
            LabelTrackTable.deleteAll()
            LabelCopyrightTable.deleteAll()
            TrackTable.deleteAll()
            LabelTable.deleteAll()
        }
    }

    "reading the tracks of a label we never recorded returns nothing and records nothing" {
        gateway.getTracksFrom(label).shouldBeEmpty()

        gateway.getLabels().shouldBeEmpty()
    }

    "saved tracks come back for their label, and the label is recorded with its copyrights" {
        gateway.saveTracks(setOf(track1, track2), label)

        gateway.getTracksFrom(label) shouldBe setOf(track1, track2)
        gateway.getLabelBy(label.name) shouldBe label
        gateway.getLabels() shouldBe setOf(label)
    }

    "saving the same tracks again is idempotent" {
        gateway.saveTracks(setOf(track1), label)
        gateway.saveTracks(setOf(track1, track2), label)

        gateway.getTracksFrom(label) shouldBe setOf(track1, track2)
        transaction(database) { LabelTrackTable.selectAll().count() } shouldBe 2L
    }

    "saving an empty set still records the label and any copyright it just learned" {
        gateway.saveTracks(emptySet(), label)
        gateway.saveTracks(emptySet(), label.copy(copyrights = setOf("This Never Happened Ltd")))

        gateway.getLabelBy(label.name)?.copyrights shouldBe setOf("This Never Happened", "This Never Happened Ltd")
        gateway.getTracksFrom(label).shouldBeEmpty()
    }
})

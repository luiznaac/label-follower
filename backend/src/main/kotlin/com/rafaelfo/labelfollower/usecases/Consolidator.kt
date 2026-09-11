package com.rafaelfo.labelfollower.usecases

import com.rafaelfo.labelfollower.models.Label
import com.rafaelfo.labelfollower.models.Track
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class Consolidator(
    private val ourInfoGateway: OurInfoGateway,
    private val labelIntrospector: LabelIntrospector,
    private val userPlaylistGateway: UserInfoGateway,
) {

    // One label at a time: find its new tracks, build the playlist, and only then record the
    // tracks as known. A failure (no Spotify account, Spotify error, anything) leaves that label's
    // tracks new — they come back on the next run — and doesn't stop the remaining labels. The
    // price is at-least-once: if the playlist is created but recording fails, the next run builds
    // another one with the same tracks. Losing tracks is worse than a duplicate playlist.
    fun introspectAllLabelsAndNotify(): ConsolidationReport {
        val report = ConsolidationReport(ourInfoGateway.getLabels().map { consolidate(it) })

        notify(report)
        if (report.failures.isNotEmpty()) throw ConsolidationFailedException(report)
        return report
    }

    @Suppress("TooGenericExceptionCaught") // isolating labels from each other is the point
    private fun consolidate(label: Label): LabelConsolidation =
        try {
            val newTracks = labelIntrospector.findNewTracksFrom(label)
            if (newTracks.isEmpty()) {
                LabelConsolidation.NothingNew(label)
            } else {
                userPlaylistGateway.createPlaylistWith(label, newTracks)
                labelIntrospector.markAsKnown(newTracks, label)
                LabelConsolidation.PlaylistCreated(label, newTracks)
            }
        } catch (error: Exception) {
            LabelConsolidation.Failed(label, error)
        }

    private fun notify(report: ConsolidationReport) {
        report.results.forEach { result ->
            when (result) {
                is LabelConsolidation.NothingNew ->
                    logger.info("{}: nothing new", result.label.name)
                is LabelConsolidation.PlaylistCreated ->
                    logger.info("{}: playlist created with {} new tracks", result.label.name, result.tracks.size)
                is LabelConsolidation.Failed ->
                    logger.error("{}: failed, its tracks stay new for the next run", result.label.name, result.error)
            }
        }
    }

    private companion object {
        val logger = LoggerFactory.getLogger(Consolidator::class.java)
    }
}

data class ConsolidationReport(val results: List<LabelConsolidation>) {
    val failures: List<LabelConsolidation.Failed> get() = results.filterIsInstance<LabelConsolidation.Failed>()
}

sealed interface LabelConsolidation {
    val label: Label

    data class NothingNew(override val label: Label) : LabelConsolidation
    data class PlaylistCreated(override val label: Label, val tracks: Set<Track>) : LabelConsolidation
    data class Failed(override val label: Label, val error: Exception) : LabelConsolidation
}

class ConsolidationFailedException(val report: ConsolidationReport) : RuntimeException(
    report.failures.joinToString(
        prefix = "Consolidation failed for ${report.failures.size} of ${report.results.size} labels " +
            "(their new tracks were kept for the next run): ",
    ) { "${it.label.name} (${it.error.message})" },
)

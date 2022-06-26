package com.rafaelfo.labelfollower.integrations.database

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.rafaelfo.labelfollower.models.Label
import com.rafaelfo.labelfollower.models.Track
import com.rafaelfo.labelfollower.usecases.OurInfoGateway
import okhttp3.Response
import org.springframework.stereotype.Component
import java.io.File

@Component
class OurInfoGatewayImpl : OurInfoGateway {

    override fun getTracksFrom(label: Label): Set<Track> {
        saveLabel(label)
        return getInfoFrom(label.getFileName(), default = emptySet())
    }

    override fun saveTracks(tracks: Set<Track>, label: Label) {
        saveInfoTo(
            label.getFileName(),
            getTracksFrom(label) + tracks
        )
    }

    override fun getLabelBy(labelName: String) : Label? {
        val labels = getLabels()
        return labels.firstOrNull { it.name == labelName }
    }

    override fun getLabels(): Set<Label> {
        return getInfoFrom(LABEL_FILENAME, emptySet())
    }

    private fun saveLabel(label: Label) {
        val newLabel = resolveLabel(label)
        val newLabels = getLabels().filter { it.name != newLabel.name } + newLabel

        saveInfoTo(LABEL_FILENAME, newLabels)
    }

    private fun resolveLabel(label: Label) : Label {
        val persistedLabel = getLabelBy(label.name) ?: return label

        return persistedLabel.copy(
            copyrights = persistedLabel.copyrights + label.copyrights
        )
    }

    private inline fun <reified T> getInfoFrom(fileName: String, default: T) : T {
        val file = File(fileName)

        if (!file.exists()) {
            return default
        }

        return file.readAsJson()
    }

    private inline fun <reified T> saveInfoTo(fileName: String, info: T) {
        GsonBuilder().setPrettyPrinting()
        File(fileName).writeText(
            gson.toJson(info)
        )
    }

    private inline fun <reified T> File.readAsJson(): T =
        gson.fromJson(readText(), object: TypeToken<T>() {}.type)

    companion object {
        private val gson = GsonBuilder().setPrettyPrinting().create()
        private const val LABEL_FILENAME = "labels.txt"
    }
}

private fun Label.getFileName() = "${name.lowercase().replace(" ", "")}.txt"

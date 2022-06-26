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
        val file = File(label.getFileName())

        if (!file.exists()) {
            return emptySet()
        }

        return file.readAsJson()
    }

    override fun saveTracks(tracks: Set<Track>, label: Label) {
        val labelTracks = getTracksFrom(label)

        File(label.getFileName()).writeText(
            GsonBuilder().create().toJson(labelTracks + tracks)
        )
    }

}

private fun Label.getFileName() = "${name.lowercase().replace(" ", "")}.txt"

private inline fun <reified T> File.readAsJson(): T =
    GsonBuilder().create().fromJson(readText(), object: TypeToken<T>() {}.type)

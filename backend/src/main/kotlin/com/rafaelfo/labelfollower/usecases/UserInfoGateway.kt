package com.rafaelfo.labelfollower.usecases

import com.rafaelfo.labelfollower.models.Label
import com.rafaelfo.labelfollower.models.Track

interface UserInfoGateway {

    fun createPlaylistWith(label: Label, tracks: Set<Track>, userToken: String)
}

package com.zords.soramusic

import com.zords.soramusic.data.sources.AndroidSourcesBehaviour
import com.zords.soramusic.data.sources.SourceRegistry

/** The phone's decoder/player answers without the encrypted store, which needs a Context. */
fun useAndroidSourcesHost() {
    SourceRegistry.host = object : AndroidSourcesBehaviour() {
        override var storedSources: String? = null
        override var jioSaavnOptInDone = false
    }
}

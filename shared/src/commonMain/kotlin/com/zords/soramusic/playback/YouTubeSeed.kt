package com.zords.soramusic.playback

import com.zords.soramusic.data.YtMusicRepository
import com.zords.soramusic.data.model.SearchFilter
import com.zords.soramusic.data.model.SearchResult
import com.zords.soramusic.data.model.Song
import com.zords.soramusic.data.sources.SourceRegistry
import com.zords.soramusic.data.sources.TrackMatcher

/**
 * Finds the YouTube id that should seed AutoPlay for a song. Module tracks do not
 * carry YouTube ids, so they are matched on YouTube before the radio request.
 */
suspend fun youtubeSeedFor(song: Song): String? {
    if (SourceRegistry.parseTrackKey(song.videoId) == null) return song.videoId
    val target = TrackMatcher.targetOf(song)
    val query = TrackMatcher.queries(target).firstOrNull() ?: return null
    return YtMusicRepository.search(query, SearchFilter.SONGS)
        .getOrNull()
        ?.filterIsInstance<SearchResult.Track>()
        ?.map { it.song }
        ?.let { TrackMatcher.best(it, target) }
        ?.videoId
}

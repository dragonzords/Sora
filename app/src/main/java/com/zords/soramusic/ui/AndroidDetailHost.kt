package com.zords.soramusic.ui

import androidx.compose.ui.graphics.vector.ImageVector
import coil3.request.ImageRequest
import coil3.request.transformations
import com.zords.soramusic.data.LocalGroups
import com.zords.soramusic.data.canvas.AppleArtistArt
import com.zords.soramusic.data.canvas.AppleArtistArtRepository
import com.zords.soramusic.data.canvas.ArtistSignature
import com.zords.soramusic.data.canvas.ArtistSignatureRepository
import com.zords.soramusic.data.canvas.CanvasArtwork
import com.zords.soramusic.data.canvas.CanvasRepository
import com.zords.soramusic.data.canvas.SignatureInkTransformation
import com.zords.soramusic.ui.screens.deviceFolderIcon
import com.zords.soramusic.ui.screens.DetailHost
import kotlinx.coroutines.flow.StateFlow

object AndroidDetailHost : DetailHost {
    override fun cachedArt(name: String): AppleArtistArt? = AppleArtistArtRepository.cached(name)
    override suspend fun artFor(name: String): AppleArtistArt? = AppleArtistArtRepository.artFor(name)
    override val signatureUpdates: StateFlow<Int> get() = ArtistSignatureRepository.updates
    override fun signatureBase(enabled: Boolean, url: String) = ArtistSignatureRepository.effectiveBaseUrl(enabled, url)
    override fun cachedSignature(name: String, base: String?): ArtistSignature? = ArtistSignatureRepository.cached(name, base)
    override suspend fun signatureFor(name: String, base: String?): ArtistSignature? = ArtistSignatureRepository.signatureFor(name, base)
    override fun ImageRequest.Builder.signatureInk() { transformations(SignatureInkTransformation) }
    override suspend fun canvasForAlbum(album: String, artist: String): CanvasArtwork? = CanvasRepository.canvasForAlbum(album, artist)
    override fun isLocalArtist(browseId: String) = LocalGroups.isArtist(browseId)
    override fun folderIcon(browseId: String): ImageVector? = deviceFolderIcon(browseId)
}

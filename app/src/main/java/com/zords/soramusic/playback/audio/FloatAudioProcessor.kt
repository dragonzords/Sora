package com.zords.soramusic.playback.audio

/**
 * Common contract for high-precision, in-place Float32 audio DSP processors.
 *
 * Processors operate directly on canonical [AudioBlock] instances holding normalized
 * IEEE 754 32-bit floats, avoiding intermediate fixed-point quantization and preserving
 * dynamic headroom throughout the DSP pipeline.
 */
interface FloatAudioProcessor {
    /**
     * Configures the processor for the specified [sampleRate] and [channelCount].
     */
    fun configure(sampleRate: Int, channelCount: Int = AudioBlock.DEFAULT_CHANNELS)

    /**
     * Processes audio frames in [block] in-place.
     *
     * @param block Reusable AudioBlock containing interleaved normalized Float32 samples.
     */
    fun process(block: AudioBlock)

    /**
     * Flushes internal filter states and histories (e.g. on seek or stream boundary).
     */
    fun flush()

    /**
     * Resets internal states and releases sample buffers.
     */
    fun reset()
}

/** Adapts [com.zords.soramusic.playback.SpatialAudioProcessor] to [FloatAudioProcessor]. */
fun com.zords.soramusic.playback.SpatialAudioProcessor.asFloatAudioProcessor(): FloatAudioProcessor =
    object : FloatAudioProcessor {
        override fun configure(sampleRate: Int, channelCount: Int) =
            this@asFloatAudioProcessor.configure(sampleRate, channelCount)
        override fun process(block: AudioBlock) =
            this@asFloatAudioProcessor.process(block)
        @Suppress("DEPRECATION")
        override fun flush() = this@asFloatAudioProcessor.flush()
        override fun reset() = this@asFloatAudioProcessor.reset()
    }

/** Adapts [com.zords.soramusic.playback.EqualizerProcessor] to [FloatAudioProcessor]. */
fun com.zords.soramusic.playback.EqualizerProcessor.asFloatAudioProcessor(): FloatAudioProcessor =
    object : FloatAudioProcessor {
        override fun configure(sampleRate: Int, channelCount: Int) =
            this@asFloatAudioProcessor.configure(sampleRate, channelCount)
        override fun process(block: AudioBlock) =
            this@asFloatAudioProcessor.process(block)
        @Suppress("DEPRECATION")
        override fun flush() = this@asFloatAudioProcessor.flush()
        override fun reset() = this@asFloatAudioProcessor.reset()
    }

/** Adapts [com.zords.soramusic.playback.TransitionFilterProcessor] to [FloatAudioProcessor]. */
fun com.zords.soramusic.playback.TransitionFilterProcessor.asFloatAudioProcessor(): FloatAudioProcessor =
    object : FloatAudioProcessor {
        override fun configure(sampleRate: Int, channelCount: Int) =
            this@asFloatAudioProcessor.configure(sampleRate, channelCount)
        override fun process(block: AudioBlock) =
            this@asFloatAudioProcessor.process(block)
        @Suppress("DEPRECATION")
        override fun flush() = this@asFloatAudioProcessor.flush()
        override fun reset() = this@asFloatAudioProcessor.reset()
    }


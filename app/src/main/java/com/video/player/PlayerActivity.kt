package com.video.player

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.Assertions
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.trackselection.MappingTrackSelector
import androidx.recyclerview.widget.LinearLayoutManager
import com.video.player.adapters.BitrateAdapter
import com.video.player.databinding.ActivityPlayerBinding
import com.video.player.models.BitrateModel

class PlayerActivity : AppCompatActivity(), BitrateAdapter.BitrateListener {

    private lateinit var binding: ActivityPlayerBinding
    private var exoPlayer: ExoPlayer? = null
    private val playUrl =
        "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"
    private var bitrateList = ArrayList<BitrateModel>()
    private lateinit var trackSelector: DefaultTrackSelector
    private lateinit var adapter: BitrateAdapter
    private var isBitrateLayoutVisible = false
    private var isBitrateFetched = false

    @UnstableApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setControllerVisibility()
    }

    @UnstableApi
    override fun onResume() {
        super.onResume()

        if (exoPlayer == null) {
            playVideo()
            setPlayerListener()
        } else {
            exoPlayer?.playWhenReady = true
        }
    }

    @UnstableApi
    private fun playVideo() {

        trackSelector = DefaultTrackSelector(this)

        exoPlayer = ExoPlayer.Builder(this).setTrackSelector(trackSelector).build()
        binding.playerView.player = exoPlayer
        exoPlayer?.setMediaItem(getMediaItem())
        exoPlayer?.prepare()
        exoPlayer?.playWhenReady = true

        binding.playerView.findViewById<ImageView>(R.id.exo_fullscreen).setOnClickListener {

        }

        binding.playerView.findViewById<ImageView>(R.id.back).setOnClickListener {
            finish()
        }

        binding.playerView.findViewById<ImageView>(R.id.exo_settings).setOnClickListener {
            if (isBitrateLayoutVisible) {
                binding.bitrateLayout.visibility = View.GONE
            } else {
                binding.bitrateLayout.visibility = View.VISIBLE
            }
            isBitrateLayoutVisible = !isBitrateLayoutVisible
        }
    }

    private fun getMediaItem(): MediaItem {
        return MediaItem.fromUri(Uri.parse(playUrl))
    }

    private fun setPlayerListener() {
        exoPlayer?.addListener(object : Player.Listener {
            @UnstableApi
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)

                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }

                    Player.STATE_ENDED -> {
                        releasePlayer()
                    }

                    Player.STATE_IDLE -> {
                        binding.progressBar.visibility = View.GONE
                    }

                    Player.STATE_READY -> {
                        binding.progressBar.visibility = View.GONE
                        if (!isBitrateFetched) {
                            getBitrate()
                        }
                    }
                }
            }
        })
        exoPlayer?.addListener(PlayerEventListener())
    }

    inner class PlayerEventListener : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)

            if (error.errorCode == 2001) {
                Toast.makeText(this@PlayerActivity, "Network Error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setControllerVisibility() {
        binding.playerView.setOnTouchListener { _, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    binding.bitrateLayout.visibility = View.GONE
                    isBitrateLayoutVisible = false
                }
            }
            false
        }
    }


    @UnstableApi
    private fun getBitrate() {
        val mappedTrackInfo = trackSelector.currentMappedTrackInfo
        var trackGroups: TrackGroupArray
        bitrateList.add(BitrateModel("Auto", true, 0))
        if (mappedTrackInfo != null) {
            for (i in 0 until mappedTrackInfo.rendererCount) {
                trackGroups = mappedTrackInfo.getTrackGroups(i)
                if (mappedTrackInfo.getRendererType(i) == C.TRACK_TYPE_VIDEO) {
                    for (j in 0 until trackGroups.length) {
                        val group = trackGroups[j]
                        for (trackIndex in 0 until group.length) {
                            if (group.getFormat(trackIndex).peakBitrate > 0) {
                                val newBitrate = "${group.getFormat(trackIndex).width} x ${
                                    group.getFormat(trackIndex).height
                                }"
                                bitrateList.add(BitrateModel(newBitrate, false, trackIndex + 1))
                            }
                        }
                    }
                }
            }
        }
        isBitrateFetched = true
        adapter.notifyDataSetChanged()
    }

    private fun setupRecyclerView() {
        adapter = BitrateAdapter(bitrateList, this)

        binding.bitrateRecyclerview.layoutManager = LinearLayoutManager(this)
        binding.bitrateRecyclerview.adapter = adapter
    }

    override fun onPause() {
        super.onPause()

        exoPlayer?.pause()
    }

    override fun onStop() {
        super.onStop()

        exoPlayer?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()

        releasePlayer()
    }

    private fun releasePlayer() {
        if (exoPlayer == null) {
            return
        }
        if (exoPlayer?.isPlaying == true) {
            exoPlayer?.stop()
        }
        exoPlayer?.release()
        exoPlayer = null
    }

    @UnstableApi
    override fun onBitrateSelect(bitrate: BitrateModel) {
        switchVideoTrack(bitrate.position)
        binding.bitrateLayout.visibility = View.GONE
        isBitrateLayoutVisible = false

        bitrateList.forEach { item ->
            item.isSelected = bitrate == item
        }
        adapter.notifyDataSetChanged()
    }

    @UnstableApi
    private fun switchVideoTrack(trackPosition: Int) {
        val mappedTrackInfo: MappingTrackSelector.MappedTrackInfo =
            Assertions.checkNotNull(trackSelector.currentMappedTrackInfo)
        val parameters = trackSelector.parameters
        val builder = parameters.buildUpon()

        for (rendererIndex in 0 until mappedTrackInfo.rendererCount) {
            val trackType = mappedTrackInfo.getRendererType(rendererIndex)
            if (trackType == C.TRACK_TYPE_VIDEO) {
                builder.clearSelectionOverrides(rendererIndex)
                    .setRendererDisabled(rendererIndex, false)
                var groupIndex = 0
                groupIndex = if (trackPosition == 0) {
                    0
                } else {
                    trackPosition - 1
                }
                val override = DefaultTrackSelector.SelectionOverride(0, groupIndex)
                builder.setSelectionOverride(
                    rendererIndex,
                    mappedTrackInfo.getTrackGroups(rendererIndex),
                    override
                )
            }
        }
        trackSelector.setParameters(builder)
    }

}
package com.video.player.models

data class BitrateModel(
    val bitrate: String?,
    var isSelected: Boolean = false,
    val position: Int
)
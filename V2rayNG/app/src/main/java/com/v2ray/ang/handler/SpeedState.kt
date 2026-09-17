package com.v2ray.ang.handler

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the current proxy speed in bytes per second as (download, upload).
 * Written by the speed-measurement loop in the service and observed by the UI.
 */
object SpeedState {
    private val _speed = MutableStateFlow(0L to 0L)
    val speed: StateFlow<Pair<Long, Long>> = _speed.asStateFlow()

    fun set(downBytesPerSec: Long, upBytesPerSec: Long) {
        _speed.value = downBytesPerSec to upBytesPerSec
    }

    fun reset() {
        _speed.value = 0L to 0L
    }
}

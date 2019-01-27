package tech.khana.reflekt.models

import android.util.Size
import android.view.Surface

/**
 * clockwise rotation, start from 180
 */
@Suppress("EnumEntryName")
enum class Rotation(val value: Int) {
    _0(0),
    _90(90),
    _180(180),
    _270(270)
}

fun displayRotationOf(value: Int) = when (value) {
    Surface.ROTATION_0 -> Rotation._0
    Surface.ROTATION_90 -> Rotation._90
    Surface.ROTATION_180 -> Rotation._180
    Surface.ROTATION_270 -> Rotation._270
    else -> throw IllegalArgumentException("unknown display rotation")
}

fun hardwareRotationOf(value: Int) = when (value) {
    0 -> Rotation._180
    90 -> Rotation._270
    180 -> Rotation._0
    270 -> Rotation._90
    else -> throw IllegalArgumentException("unknown hardware rotation")
}

fun Size.toResolution() = Resolution(width, height)

package tech.khana.reflekt.models

enum class AspectRatio(val value: Float) {
    AR_16X9(16f / 9f),
    AR_4X3(4f / 3f),
    AR_2X1(2f),
    AR_1X1(1f)
}

fun aspectRatioBy(value: Float): AspectRatio = AspectRatio.values().first { it.value == value }

fun aspectRatioBy(resolution: Resolution): AspectRatio =
    aspectRatioBy(resolution.width.toFloat() / resolution.height.toFloat())

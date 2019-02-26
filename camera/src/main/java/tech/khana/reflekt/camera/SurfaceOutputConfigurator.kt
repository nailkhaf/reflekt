package tech.khana.reflekt.camera

import android.hardware.camera2.CameraCharacteristics
import android.util.Size
import tech.khana.reflekt.api.Surface
import tech.khana.reflekt.api.models.OutputType
import tech.khana.reflekt.api.models.OutputType.*
import tech.khana.reflekt.api.models.SupportLevel
import tech.khana.reflekt.api.models.SurfaceFormat
import tech.khana.reflekt.api.models.SurfaceFormat.Image
import tech.khana.reflekt.api.models.SurfaceFormat.Priv
import tech.khana.reflekt.camera.extensions.isHardwareLevelSupported
import tech.khana.reflekt.camera.extensions.outputResolutions
import kotlin.reflect.KClass

private const val MAX_SURFACE_COUNT = 3
const val MAX_PREVIEW_WIDTH = 1920
const val MAX_PREVIEW_HEIGHT = 1080

internal class SurfaceOutputConfigurator(
    private val cameraCharacteristics: CameraCharacteristics,
    private val maxPreviewSize: Size,
    private val maxRecordSize: Size,
    surfaces: List<Surface>
) {

    private val legacyMatch: Map<List<KClass<out SurfaceFormat>?>, Array<out OutputType?>> = mapOf(

        listOf(Priv::class, null, null) to arrayOf(MAXIMUM, null, null),
        listOf(Image.Jpeg::class, null, null) to arrayOf(MAXIMUM, null, null),
        listOf(Image.Yuv::class, null, null) to arrayOf(MAXIMUM, null, null),

        listOf(Priv::class, Image.Jpeg::class, null) to arrayOf(PREVIEW, MAXIMUM, null),
        listOf(Image.Yuv::class, Image.Jpeg::class, null) to arrayOf(PREVIEW, MAXIMUM, null),
        listOf(Priv::class, Priv::class, null) to arrayOf(PREVIEW, PREVIEW, null),
        listOf(Priv::class, Image.Yuv::class, null) to arrayOf(PREVIEW, PREVIEW, null),

        listOf(Priv::class, Image.Yuv::class, Image.Jpeg::class) to arrayOf(PREVIEW, PREVIEW, MAXIMUM)
    )

    private val limitedMatch: Map<List<KClass<out SurfaceFormat>?>, Array<out OutputType?>> = mapOf(

        listOf(Priv::class, Priv::class, null) to arrayOf(PREVIEW, RECORD, null),
        listOf(Priv::class, Image.Yuv::class, null) to arrayOf(PREVIEW, RECORD, null),
        listOf(Image.Yuv::class, Image.Yuv::class, null) to arrayOf(PREVIEW, RECORD, null),

        listOf(Priv::class, Priv::class, Image.Jpeg::class) to arrayOf(PREVIEW, RECORD, RECORD),
        listOf(Priv::class, Image.Yuv::class, Image.Jpeg::class) to arrayOf(PREVIEW, RECORD, RECORD),
        listOf(Image.Yuv::class, Image.Yuv::class, Image.Jpeg::class) to arrayOf(PREVIEW, PREVIEW, MAXIMUM)
    )

    private val fullMatch: Map<List<KClass<out SurfaceFormat>?>, Array<out OutputType?>> = mapOf(

        listOf(Priv::class, Priv::class, null) to arrayOf(PREVIEW, MAXIMUM, null),
        listOf(Priv::class, Image.Yuv::class, null) to arrayOf(PREVIEW, MAXIMUM, null),
        listOf(Image.Yuv::class, Image.Yuv::class, null) to arrayOf(PREVIEW, MAXIMUM, null),

        listOf(Priv::class, Priv::class, Image.Jpeg::class) to arrayOf(PREVIEW, PREVIEW, MAXIMUM)
    )

    private val configs = mutableListOf<Map<Surface, OutputType>>()

    init {
        check(surfaces.size in 1..MAX_SURFACE_COUNT) {
            "max count of surfaces is $MAX_SURFACE_COUNT"
        }

        val tableKey = (0 until MAX_SURFACE_COUNT)
            .map { surfaces.getOrNull(it) }
            .map { it?.format }
            .map { it?.run { if (it is Priv) Priv::class else it::class } }


        if (cameraCharacteristics.isHardwareLevelSupported(SupportLevel.LEGACY)) {
            legacyMatch[tableKey]?.let {
                configs += toMap(surfaces, it)
            }
        }

        if (cameraCharacteristics.isHardwareLevelSupported(SupportLevel.LIMITED)) {
            limitedMatch[tableKey]?.let {
                configs += toMap(surfaces, it)
            }
        }

        if (cameraCharacteristics.isHardwareLevelSupported(SupportLevel.FULL)) {
            fullMatch[tableKey]?.let {
                configs += toMap(surfaces, it)
            }
        }

        check(configs.isNotEmpty()) { "unknown surface configuration" }
    }

    private fun toMap(
        surfaces: List<Surface>,
        value: Array<out OutputType?>
    ): Map<Surface, OutputType> {
        return surfaces.mapIndexed { index, reflektSurface ->
            val outputType = value[index]
            checkNotNull(outputType)
            reflektSurface to outputType
        }.toMap()
    }

    fun getOutputSizes(surface: Surface): List<Size> {

        val outputType = configs[0][surface] ?: error("surface note found")

        val outputResolutions = when (val format = surface.format) {
            is SurfaceFormat.Image ->
                cameraCharacteristics.outputResolutions(format.format)
            is SurfaceFormat.Priv ->
                cameraCharacteristics.outputResolutions(format.klass)
        }

        return when (outputType) {
            OutputType.MAXIMUM -> outputResolutions

            OutputType.RECORD -> outputResolutions.asSequence()
                .filter { it.width <= maxRecordSize.width }
                .filter { it.height <= maxRecordSize.height }
                .toList()

            OutputType.PREVIEW -> outputResolutions.asSequence()
                .filter { it.width <= maxPreviewSize.width }
                .filter { it.height <= maxPreviewSize.height }
                .filter { it.width <= MAX_PREVIEW_WIDTH }
                .filter { it.height <= MAX_PREVIEW_HEIGHT }
                .toList()
        }
    }
}
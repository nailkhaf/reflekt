package tech.khana.reflekt.core

import tech.khana.reflekt.models.OutputType
import tech.khana.reflekt.models.OutputType.*
import tech.khana.reflekt.models.ReflektFormat
import tech.khana.reflekt.models.ReflektFormat.Image
import tech.khana.reflekt.models.ReflektFormat.Priv
import tech.khana.reflekt.models.SupportLevel
import kotlin.reflect.KClass

private const val MAX_SURFACE_COUNT = 3

class DefaultSurfaceOutputConfigurator(
    level: SupportLevel,
    surfaces: List<ReflektSurface>
) : SurfaceOutputConfigurator {

    private val legacyMatch: Map<Array<out KClass<out ReflektFormat>?>, Array<out OutputType?>> = mapOf(

        arrayOf(Priv::class, null, null) to arrayOf(MAXIMUM, null, null),
        arrayOf(Image.Jpeg::class, null, null) to arrayOf(MAXIMUM, null, null),
        arrayOf(Image.Yuv::class, null, null) to arrayOf(MAXIMUM, null, null),

        arrayOf(Priv::class, Image.Jpeg::class, null) to arrayOf(PREVIEW, MAXIMUM, null),
        arrayOf(Image.Yuv::class, Image.Jpeg::class, null) to arrayOf(PREVIEW, MAXIMUM, null),
        arrayOf(Priv::class, Priv::class, null) to arrayOf(PREVIEW, PREVIEW, null),
        arrayOf(Priv::class, Image.Yuv::class, null) to arrayOf(PREVIEW, PREVIEW, null),

        arrayOf(Priv::class, Image.Yuv::class, Image.Jpeg::class) to arrayOf(PREVIEW, PREVIEW, MAXIMUM)
    )

    private val limitedMatch: Map<Array<out KClass<out ReflektFormat>?>, Array<out OutputType?>> = mapOf(

        arrayOf(Priv::class, Priv::class, null) to arrayOf(PREVIEW, RECORD, null),
        arrayOf(Priv::class, Image.Yuv::class, null) to arrayOf(PREVIEW, RECORD, null),
        arrayOf(Image.Yuv::class, Image.Yuv::class, null) to arrayOf(PREVIEW, RECORD, null),

        arrayOf(Priv::class, Priv::class, Image.Jpeg::class) to arrayOf(PREVIEW, RECORD, RECORD),
        arrayOf(Priv::class, Image.Yuv::class, Image.Jpeg::class) to arrayOf(PREVIEW, RECORD, RECORD),
        arrayOf(Image.Yuv::class, Image.Yuv::class, Image.Jpeg::class) to arrayOf(PREVIEW, PREVIEW, MAXIMUM)
    )

    private val fullMatch: Map<Array<out KClass<out ReflektFormat>?>, Array<out OutputType?>> = mapOf(

        arrayOf(Priv::class, Priv::class, null) to arrayOf(PREVIEW, MAXIMUM, null),
        arrayOf(Priv::class, Image.Yuv::class, null) to arrayOf(PREVIEW, MAXIMUM, null),
        arrayOf(Image.Yuv::class, Image.Yuv::class, null) to arrayOf(PREVIEW, MAXIMUM, null),

        arrayOf(Priv::class, Priv::class, Image.Jpeg::class) to arrayOf(PREVIEW, PREVIEW, MAXIMUM)
    )

    private val map: Map<ReflektSurface, OutputType>

    init {
        val notNoneFormatSurfaces = surfaces.filter { it.format !is ReflektFormat.None }
        check(notNoneFormatSurfaces.size in 1 until MAX_SURFACE_COUNT) {
            "max count of surfaces is $MAX_SURFACE_COUNT"
        }

        val tableKey = (0..MAX_SURFACE_COUNT)
            .map { notNoneFormatSurfaces.getOrNull(it) }
            .map { it?.format }
            .map { if (it != null) it::class else null }
            .toTypedArray()

        var value: Array<out OutputType?>? = null

        if (level.ordinal >= SupportLevel.LEGACY.ordinal) {
            value = legacyMatch[tableKey]
        }
        if (level.ordinal >= SupportLevel.LIMIT.ordinal) {
            value = limitedMatch[tableKey]
        }
        if (level.ordinal >= SupportLevel.FULL.ordinal) {
            value = fullMatch[tableKey]
        }

        checkNotNull(value) { "not found surface matching in table" }

        map = notNoneFormatSurfaces.mapIndexed { index, reflektSurface ->
            val outputType = value[index]
            checkNotNull(outputType)
            reflektSurface to outputType
        }.toMap()
    }

    override fun defineOutputType(surface: ReflektSurface): OutputType = map[surface]
        ?: error("can't get output type")
}
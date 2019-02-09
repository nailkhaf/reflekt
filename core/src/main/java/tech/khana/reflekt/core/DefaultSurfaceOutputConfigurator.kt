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

    private val legacyMatch: Map<List<KClass<out ReflektFormat>?>, Array<out OutputType?>> = mapOf(

        listOf(Priv::class, null, null) to arrayOf(MAXIMUM, null, null),
        listOf(Image.Jpeg::class, null, null) to arrayOf(MAXIMUM, null, null),
        listOf(Image.Yuv::class, null, null) to arrayOf(MAXIMUM, null, null),

        listOf(Priv::class, Image.Jpeg::class, null) to arrayOf(PREVIEW, MAXIMUM, null),
        listOf(Image.Yuv::class, Image.Jpeg::class, null) to arrayOf(PREVIEW, MAXIMUM, null),
        listOf(Priv::class, Priv::class, null) to arrayOf(PREVIEW, PREVIEW, null),
        listOf(Priv::class, Image.Yuv::class, null) to arrayOf(PREVIEW, PREVIEW, null),

        listOf(Priv::class, Image.Yuv::class, Image.Jpeg::class) to arrayOf(PREVIEW, PREVIEW, MAXIMUM)
    )

    private val limitedMatch: Map<List<KClass<out ReflektFormat>?>, Array<out OutputType?>> = mapOf(

        listOf(Priv::class, Priv::class, null) to arrayOf(PREVIEW, RECORD, null),
        listOf(Priv::class, Image.Yuv::class, null) to arrayOf(PREVIEW, RECORD, null),
        listOf(Image.Yuv::class, Image.Yuv::class, null) to arrayOf(PREVIEW, RECORD, null),

        listOf(Priv::class, Priv::class, Image.Jpeg::class) to arrayOf(PREVIEW, RECORD, RECORD),
        listOf(Priv::class, Image.Yuv::class, Image.Jpeg::class) to arrayOf(PREVIEW, RECORD, RECORD),
        listOf(Image.Yuv::class, Image.Yuv::class, Image.Jpeg::class) to arrayOf(PREVIEW, PREVIEW, MAXIMUM)
    )

    private val fullMatch: Map<List<KClass<out ReflektFormat>?>, Array<out OutputType?>> = mapOf(

        listOf(Priv::class, Priv::class, null) to arrayOf(PREVIEW, MAXIMUM, null),
        listOf(Priv::class, Image.Yuv::class, null) to arrayOf(PREVIEW, MAXIMUM, null),
        listOf(Image.Yuv::class, Image.Yuv::class, null) to arrayOf(PREVIEW, MAXIMUM, null),

        listOf(Priv::class, Priv::class, Image.Jpeg::class) to arrayOf(PREVIEW, PREVIEW, MAXIMUM)
    )

    private val map: Map<ReflektSurface, OutputType>

    init {
        val notNoneFormatSurfaces = surfaces.filter { it.format !is ReflektFormat.None }
        check(notNoneFormatSurfaces.size in 1..MAX_SURFACE_COUNT) {
            "max count of surfaces is $MAX_SURFACE_COUNT"
        }

        val tableKey = (0 until MAX_SURFACE_COUNT)
            .map { notNoneFormatSurfaces.getOrNull(it) }
            .map { it?.format }
            .map { it?.run { if (it is Priv) Priv::class else it::class } }

        var value: Array<out OutputType?>? = null

        if (level.ordinal >= SupportLevel.LEGACY.ordinal) {
            value = legacyMatch[tableKey] ?: value
        }
        if (level.ordinal >= SupportLevel.LIMIT.ordinal) {
            value = limitedMatch[tableKey] ?: value
        }
        if (level.ordinal >= SupportLevel.FULL.ordinal) {
            value = fullMatch[tableKey] ?: value
        }

        checkNotNull(value) { "unknown surface configuration" }

        map = notNoneFormatSurfaces.mapIndexed { index, reflektSurface ->
            val outputType = value[index]
            checkNotNull(outputType)
            reflektSurface to outputType
        }.toMap()
    }

    override fun defineOutputType(surface: ReflektSurface): OutputType = map[surface]
        ?: error("can't get output type")
}
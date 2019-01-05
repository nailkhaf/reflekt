package tech.khana.reflekt.core

interface ReflektDevice {

    suspend fun open()

    suspend fun startPreview()

    suspend fun stopPreview()

    suspend fun capture()

    suspend fun flash(mode: FlashMode)

    suspend fun zoom(float: Float)

    suspend fun release()
}

interface ReflektCamera {

    suspend fun open()

    suspend fun startPreview()

    suspend fun stop()

    fun getAvailableLenses(): List<Lens>
}

interface CameraSurface {

    val format: LensFormat

    suspend fun acquireSurface(config: SurfaceConfig): TypedSurface

    fun stop() {
    }
}

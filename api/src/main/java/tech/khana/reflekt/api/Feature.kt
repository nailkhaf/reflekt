package tech.khana.reflekt.api

import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import tech.khana.reflekt.api.models.CameraMode
import java.lang.ref.WeakReference
import kotlin.reflect.KClass

interface Feature<T> {

    val available: List<T>

    val supportedModes: Set<CameraMode>

    fun bind(session: Session)

    fun unbind(session: Session)

    fun prepareRequest(capture: CaptureRequest.Builder)

    fun readResult(result: CaptureResult)

    fun change(e: T)
}

interface FeatureHolder {

    fun getFeature(klass: KClass<out Feature<*>>): Feature<*>
}

inline fun <reified F : Feature<*>> FeatureHolder.getFeature(): Feature<*> {
    return getFeature(F::class)
}

interface FeatureFactory {

    operator fun invoke(
        session: WeakReference<Session>
    ): Feature<*>
}

object NotSupportedFeature : Feature<Nothing> {

    override val available: List<Nothing> = emptyList()

    override val supportedModes: Set<CameraMode> = emptySet()

    override fun prepareRequest(capture: CaptureRequest.Builder) {
    }

    override fun readResult(result: CaptureResult) {
    }

    override fun bind(session: Session) {
    }

    override fun unbind(session: Session) {
    }

    override fun change(e: Nothing) {
        throw UnsupportedOperationException()
    }
}
package tech.khana.reflekt.core

import android.graphics.Rect
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

internal class SettingsProviderImpl(
    settings: ReflektSettings,
    private val dispatcher: CoroutineDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
) : SettingsProvider {

    override val currentSettings: ReflektSettings
        get() = settings.get()

    private val settings = AtomicReference(settings)

    override suspend fun flash(flashMode: FlashMode) = changeState {
        it.copy(flashMode = flashMode)
    }

    override suspend fun lens(lens: Lens) = changeState {
        it.copy(lens = lens)
    }

    override suspend fun supportLevel(supportLevel: SupportLevel) = changeState {
        it.copy(supportLevel = supportLevel)
    }

    override suspend fun previewActive(active: Boolean) = changeState {
        it.copy(previewActive = active)
    }

    override suspend fun previewAspectRation(aspectRatio: AspectRatio) = changeState {
        it.copy(previewAspectRatio = aspectRatio)
    }

    override suspend fun sessionActive(active: Boolean) = changeState {
        it.copy(sessionActive = active)
    }

    override suspend fun zoom(zoom: Float) = changeState {
        it.copy(zoom = zoom)
    }

    override suspend fun sensorRect(sensorRect: Rect) = changeState {
        it.copy(sensorRect = sensorRect)
    }

    private suspend inline fun changeState(crossinline block: (ReflektSettings) -> ReflektSettings) = coroutineScope {
        withContext(dispatcher) {
            settings.ref = block(settings.ref)
        }
    }
}

private var <V> AtomicReference<V>.ref: V
    get() = get()
    set(value) = set(value)
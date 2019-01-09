package tech.khana.reflekt.core

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

    override suspend fun flash(flashMode: FlashMode) = coroutineScope {
        withContext(dispatcher) {
            settings.ref = settings.ref.copy(flashMode = flashMode)
        }
    }

    override suspend fun lens(lens: Lens) = coroutineScope {
        withContext(dispatcher) {
            settings.ref = settings.ref.copy(lens = lens)
        }
    }

    override suspend fun supportLevel(supportLevel: SupportLevel) = coroutineScope {
        withContext(dispatcher) {
            settings.ref = settings.ref.copy(supportLevel = supportLevel)
        }
    }

    override suspend fun previewActive(active: Boolean) = coroutineScope {
        withContext(dispatcher) {
            settings.ref = settings.ref.copy(previewActive = active)
        }
    }

    override suspend fun previewAspectRation(aspectRatio: AspectRatio) = coroutineScope {
        withContext(dispatcher) {
            settings.ref = settings.ref.copy(previewAspectRatio = aspectRatio)
        }
    }

    override suspend fun sessionActive(active: Boolean) = coroutineScope {
        withContext(dispatcher) {
            settings.ref = settings.ref.copy(sessionActive = active)
        }
    }
}

private var <V> AtomicReference<V>.ref: V
    get() = get()
    set(value) = set(value)
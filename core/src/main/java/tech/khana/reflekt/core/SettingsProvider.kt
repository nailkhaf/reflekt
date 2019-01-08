package tech.khana.reflekt.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

internal class SettingsProviderImpl(
    reflektSettings: ReflektSettings,
    private val dispatcher: CoroutineDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
) : SettingsProvider {

    override val currentSettings: ReflektSettings
        get() = _currentSettings.get()

    private val _currentSettings = AtomicReference(reflektSettings)

    override suspend fun flash(flashMode: FlashMode) = coroutineScope {
        withContext(dispatcher) {
            _currentSettings.set(currentSettings.copy(flashMode = flashMode))
        }
    }

    override suspend fun supportLevel(supportLevel: SupportLevel) = coroutineScope {
        withContext(dispatcher) {
            _currentSettings.set(currentSettings.copy(supportLevel = supportLevel))
        }
    }

    override suspend fun previewActive(active: Boolean) = coroutineScope {
        withContext(dispatcher) {
            _currentSettings.set(currentSettings.copy(previewActive = active))
        }
    }

    override suspend fun previewAspectRation(aspectRatio: AspectRatio) = coroutineScope {
        withContext(dispatcher) {
            _currentSettings.set(currentSettings.copy(previewAspectRatio = aspectRatio))
        }
    }

    override suspend fun sessionActive(active: Boolean) = coroutineScope {
        withContext(dispatcher) {
            _currentSettings.set(currentSettings.copy(sessionActive = active))
        }
    }
}
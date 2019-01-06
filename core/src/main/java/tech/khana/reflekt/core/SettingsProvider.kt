package tech.khana.reflekt.core

internal class SettingsProviderImpl(userSettings: UserSettings) : SettingsProvider {

    override var currentSettings: UserSettings = userSettings

    override fun flash(flashMode: FlashMode) {
        currentSettings = currentSettings.copy(flashMode = flashMode)
    }

    override fun supportLevel(supportLevel: SupportLevel) {
        currentSettings = currentSettings.copy(supportLevel = supportLevel)
    }
}
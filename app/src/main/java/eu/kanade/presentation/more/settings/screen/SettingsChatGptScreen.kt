package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import eu.kanade.domain.chatgpt.service.ChatGptIntegrationPreferences
import eu.kanade.presentation.more.settings.Preference
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.i18n.kmk.KMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

// KMK -->
object SettingsChatGptScreen : SearchableSettings {
    @Suppress("unused")
    private fun readResolve(): Any = SettingsChatGptScreen

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = KMR.strings.chatgpt_integration_title

    @Composable
    override fun getPreferences(): List<Preference> {
        val preferences = remember { Injekt.get<ChatGptIntegrationPreferences>() }
        val enabled by preferences.enabled().collectAsState()

        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(KMR.strings.chatgpt_integration_group),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = preferences.enabled(),
                        title = stringResource(KMR.strings.chatgpt_integration_enable),
                        subtitle = stringResource(KMR.strings.chatgpt_integration_enable_summary),
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(KMR.strings.chatgpt_integration_connection),
                        subtitle = stringResource(
                            if (enabled) {
                                KMR.strings.chatgpt_integration_enabled_unpaired
                            } else {
                                KMR.strings.chatgpt_integration_not_enabled
                            },
                        ),
                        enabled = false,
                    ),
                    Preference.PreferenceItem.InfoPreference(
                        stringResource(KMR.strings.chatgpt_integration_pairing_unavailable),
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(KMR.strings.chatgpt_integration_permissions_group),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.InfoPreference(
                        stringResource(KMR.strings.chatgpt_integration_permissions_summary),
                    ),
                    Preference.PreferenceItem.InfoPreference(
                        stringResource(KMR.strings.chatgpt_integration_data_summary),
                    ),
                ),
            ),
        )
    }
}
// KMK <--

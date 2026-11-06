package eu.kanade.presentation.more.settings.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import eu.kanade.domain.chatgpt.service.ChatGptDeviceIdentity
import eu.kanade.domain.chatgpt.service.ChatGptDeviceIdentityInfo
import eu.kanade.domain.chatgpt.service.ChatGptIntegrationPreferences
import eu.kanade.presentation.more.settings.Preference
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tachiyomi.i18n.MR
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
        val deviceIdentity = remember { Injekt.get<ChatGptDeviceIdentity>() }
        val enabled by preferences.enabled().collectAsState()
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        var identity by remember { mutableStateOf<ChatGptDeviceIdentityInfo?>(null) }
        var identityLoading by remember { mutableStateOf(true) }
        var identityWorking by remember { mutableStateOf(false) }
        var identityReadFailed by remember { mutableStateOf(false) }
        var showRemoveIdentityDialog by remember { mutableStateOf(false) }

        LaunchedEffect(deviceIdentity) {
            try {
                identity = withContext(Dispatchers.IO) {
                    deviceIdentity.getExistingIdentity()
                }
            } catch (_: Exception) {
                identityReadFailed = true
            } finally {
                identityLoading = false
            }
        }

        if (showRemoveIdentityDialog) {
            AlertDialog(
                onDismissRequest = { showRemoveIdentityDialog = false },
                title = { Text(stringResource(KMR.strings.chatgpt_integration_identity_remove_title)) },
                text = { Text(stringResource(KMR.strings.chatgpt_integration_identity_remove_body)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showRemoveIdentityDialog = false
                            scope.launch {
                                identityWorking = true
                                try {
                                    withContext(Dispatchers.IO) {
                                        deviceIdentity.removeLocalIdentity()
                                    }
                                    identity = null
                                } catch (_: Exception) {
                                    identityReadFailed = true
                                } finally {
                                    identityWorking = false
                                }
                            }
                        },
                    ) {
                        Text(stringResource(KMR.strings.chatgpt_integration_identity_remove_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRemoveIdentityDialog = false }) {
                        Text(stringResource(MR.strings.action_cancel))
                    }
                },
            )
        }

        fun copyToClipboard(label: String, value: String) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        }

        val identityItems = when {
            identityLoading || identityWorking -> persistentListOf(
                Preference.PreferenceItem.InfoPreference(
                    stringResource(KMR.strings.chatgpt_integration_identity_loading),
                ),
            )
            identityReadFailed -> persistentListOf(
                Preference.PreferenceItem.InfoPreference(
                    stringResource(KMR.strings.chatgpt_integration_identity_read_error),
                ),
            )
            identity == null -> persistentListOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(KMR.strings.chatgpt_integration_identity_create),
                    subtitle = stringResource(KMR.strings.chatgpt_integration_identity_create_summary),
                    enabled = enabled && !identityWorking,
                    onClick = {
                        scope.launch {
                            identityWorking = true
                            try {
                                identity = withContext(Dispatchers.IO) {
                                    deviceIdentity.getOrCreateIdentity()
                                }
                                identityReadFailed = false
                            } catch (_: Exception) {
                                identityReadFailed = true
                            } finally {
                                identityWorking = false
                            }
                        }
                    },
                ),
                Preference.PreferenceItem.InfoPreference(
                    stringResource(KMR.strings.chatgpt_integration_identity_not_created),
                ),
            )
            else -> persistentListOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(KMR.strings.chatgpt_integration_identity_fingerprint),
                    subtitle = identity!!.fingerprint,
                    onClick = {
                        copyToClipboard(
                            "Komikku ChatGPT device fingerprint",
                            identity!!.fingerprint,
                        )
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(KMR.strings.chatgpt_integration_identity_copy_public_key),
                    subtitle = stringResource(KMR.strings.chatgpt_integration_identity_copy_public_key_summary),
                    onClick = {
                        copyToClipboard(
                            "Komikku ChatGPT public key",
                            identity!!.publicKeyBase64,
                        )
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(KMR.strings.chatgpt_integration_identity_remove),
                    subtitle = stringResource(KMR.strings.chatgpt_integration_identity_remove_summary),
                    onClick = { showRemoveIdentityDialog = true },
                ),
                Preference.PreferenceItem.InfoPreference(
                    stringResource(KMR.strings.chatgpt_integration_identity_ready),
                ),
            )
        }

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
                title = stringResource(KMR.strings.chatgpt_integration_identity_group),
                preferenceItems = identityItems,
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

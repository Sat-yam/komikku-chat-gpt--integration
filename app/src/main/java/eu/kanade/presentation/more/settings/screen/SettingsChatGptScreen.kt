package eu.kanade.presentation.more.settings.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import eu.kanade.domain.chatgpt.service.ChatGptDeviceIdentity
import eu.kanade.domain.chatgpt.service.ChatGptDeviceIdentityInfo
import eu.kanade.domain.chatgpt.service.ChatGptIntegrationPreferences
import eu.kanade.domain.chatgpt.service.ChatGptPairingQrCodeGenerator
import eu.kanade.domain.chatgpt.service.ChatGptPairingRequest
import eu.kanade.domain.chatgpt.service.ChatGptPairingRequestFactory
import eu.kanade.presentation.more.settings.Preference
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
        val pairingRequestFactory = remember { Injekt.get<ChatGptPairingRequestFactory>() }
        val pairingQrGenerator = remember { Injekt.get<ChatGptPairingQrCodeGenerator>() }
        val enabled by preferences.enabled().collectAsState()
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        var identity by remember { mutableStateOf<ChatGptDeviceIdentityInfo?>(null) }
        var identityLoading by remember { mutableStateOf(true) }
        var identityWorking by remember { mutableStateOf(false) }
        var identityReadFailed by remember { mutableStateOf(false) }
        var showRemoveIdentityDialog by remember { mutableStateOf(false) }
        var pairingRequest by remember { mutableStateOf<ChatGptPairingRequest?>(null) }
        var pairingQrBitmap by remember { mutableStateOf<Bitmap?>(null) }
        var pairingGenerating by remember { mutableStateOf(false) }
        var pairingGenerationFailed by remember { mutableStateOf(false) }
        var pairingNowSeconds by remember { mutableStateOf(System.currentTimeMillis() / 1_000L) }

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

        LaunchedEffect(pairingRequest?.requestId) {
            val requestId = pairingRequest?.requestId ?: return@LaunchedEffect
            while (pairingRequest?.requestId == requestId) {
                pairingNowSeconds = System.currentTimeMillis() / 1_000L
                delay(1_000L)
            }
        }

        fun createPairingRequest() {
            val currentIdentity = identity ?: return
            scope.launch {
                pairingGenerating = true
                pairingGenerationFailed = false
                try {
                    val requestAndBitmap = withContext(Dispatchers.Default) {
                        val request = pairingRequestFactory.create(currentIdentity)
                        val bitmap = pairingQrGenerator.generate(request.payloadJson)
                        request to bitmap
                    }
                    pairingRequest = requestAndBitmap.first
                    pairingQrBitmap = requestAndBitmap.second
                    pairingNowSeconds = System.currentTimeMillis() / 1_000L
                } catch (_: Exception) {
                    pairingGenerationFailed = true
                } finally {
                    pairingGenerating = false
                }
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
                                    pairingRequest = null
                                    pairingQrBitmap = null
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

        pairingRequest?.let { request ->
            val remainingSeconds = request.remainingSeconds(pairingNowSeconds)
            val isExpired = remainingSeconds <= 0L
            val countdown = "${remainingSeconds / 60}:${(remainingSeconds % 60).toString().padStart(2, '0')}"

            AlertDialog(
                onDismissRequest = {
                    pairingRequest = null
                    pairingQrBitmap = null
                },
                title = {
                    Text(stringResource(KMR.strings.chatgpt_pairing_request_title))
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        pairingQrBitmap?.let { bitmap ->
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = stringResource(KMR.strings.chatgpt_pairing_qr_content_description),
                                modifier = Modifier.size(224.dp),
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(stringResource(KMR.strings.chatgpt_pairing_request_code))
                        Text(
                            text = request.requestCode,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isExpired) {
                                stringResource(KMR.strings.chatgpt_pairing_request_expired)
                            } else {
                                stringResource(KMR.strings.chatgpt_pairing_request_expires, countdown)
                            },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(KMR.strings.chatgpt_pairing_request_local_only))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(KMR.strings.chatgpt_pairing_request_fingerprint, request.fingerprint))
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = {
                                copyToClipboard("Komikku ChatGPT pairing code", request.requestCode)
                            },
                            enabled = !isExpired,
                        ) {
                            Text(stringResource(KMR.strings.chatgpt_pairing_copy_code))
                        }
                        TextButton(
                            onClick = {
                                pairingRequest = null
                                pairingQrBitmap = null
                                createPairingRequest()
                            },
                        ) {
                            Text(stringResource(KMR.strings.chatgpt_pairing_regenerate))
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            pairingRequest = null
                            pairingQrBitmap = null
                        },
                    ) {
                        Text(stringResource(MR.strings.action_cancel))
                    }
                },
            )
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
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(KMR.strings.chatgpt_integration_identity_group),
                preferenceItems = identityItems,
            ),

            Preference.PreferenceGroup(
                title = stringResource(KMR.strings.chatgpt_pairing_group),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(KMR.strings.chatgpt_pairing_generate),
                        subtitle = stringResource(
                            if (identity == null) {
                                KMR.strings.chatgpt_pairing_create_identity_first
                            } else {
                                KMR.strings.chatgpt_pairing_generate_summary
                            },
                        ),
                        enabled = enabled && identity != null && !identityLoading && !identityWorking && !pairingGenerating,
                        onClick = { createPairingRequest() },
                    ),
                    Preference.PreferenceItem.InfoPreference(
                        stringResource(
                            if (pairingGenerationFailed) {
                                KMR.strings.chatgpt_pairing_generation_failed
                            } else {
                                KMR.strings.chatgpt_pairing_relay_not_configured
                            },
                        ),
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

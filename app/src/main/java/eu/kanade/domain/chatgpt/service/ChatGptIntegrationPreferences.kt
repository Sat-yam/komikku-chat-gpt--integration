package eu.kanade.domain.chatgpt.service

import tachiyomi.core.common.preference.PreferenceStore

/**
 * Local opt-in settings for the optional ChatGPT integration.
 *
 * Enabling this preference does not create a network connection or pair a
 * remote client. Those capabilities are provided by later integration
 * milestones and remain disabled until implemented.
 */
class ChatGptIntegrationPreferences(
    private val preferenceStore: PreferenceStore,
) {
    fun enabled() = preferenceStore.getBoolean("pref_chatgpt_integration_enabled", false)
}

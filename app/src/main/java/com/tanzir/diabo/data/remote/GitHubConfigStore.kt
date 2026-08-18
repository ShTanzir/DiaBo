package com.tanzir.diabo.data.remote

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class GitHubConfig(
    val token: String,
    val owner: String,
    val repo: String,
    val branch: String = "main",
    val workflowFile: String = "diabo-preview-build.yml"
)

/**
 * Stores the GitHub Personal Access Token + template-repo config using
 * EncryptedSharedPreferences (AES-256-GCM backed by the Android Keystore) —
 * a raw PAT must never touch plain SharedPreferences or Room.
 */
@Singleton
class GitHubConfigStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "diabo_github_secure_prefs",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun save(config: GitHubConfig) {
        prefs.edit()
            .putString(KEY_TOKEN, config.token)
            .putString(KEY_OWNER, config.owner)
            .putString(KEY_REPO, config.repo)
            .putString(KEY_BRANCH, config.branch)
            .putString(KEY_WORKFLOW, config.workflowFile)
            .apply()
    }

    fun load(): GitHubConfig? {
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        val owner = prefs.getString(KEY_OWNER, null) ?: return null
        val repo = prefs.getString(KEY_REPO, null) ?: return null
        val branch = prefs.getString(KEY_BRANCH, "main") ?: "main"
        val workflow = prefs.getString(KEY_WORKFLOW, "diabo-preview-build.yml") ?: "diabo-preview-build.yml"
        return GitHubConfig(token, owner, repo, branch, workflow)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun isConfigured(): Boolean = load() != null

    companion object {
        private const val KEY_TOKEN = "pat"
        private const val KEY_OWNER = "owner"
        private const val KEY_REPO = "repo"
        private const val KEY_BRANCH = "branch"
        private const val KEY_WORKFLOW = "workflow"
    }
}

package com.tanzir.diabo.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanzir.diabo.data.backup.BackupRepository
import com.tanzir.diabo.data.backup.BackupResult
import com.tanzir.diabo.data.remote.GitHubConfig
import com.tanzir.diabo.data.remote.GitHubConfigStore
import com.tanzir.diabo.util.LanguageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val token: String = "",
    val owner: String = "",
    val repo: String = "",
    val branch: String = "main",
    val workflowFile: String = "diabo-preview-build.yml",
    val isConfigured: Boolean = false,
    val savedMessage: String? = null,
    val backupMessage: String? = null,
    val isBangla: Boolean = LanguageManager.isBangla()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val configStore: GitHubConfigStore,
    private val backupRepository: BackupRepository
) : ViewModel() {

    var uiState = androidx.compose.runtime.mutableStateOf(loadInitial())
        private set

    private fun loadInitial(): SettingsUiState {
        val config = configStore.load()
        return if (config != null) {
            SettingsUiState(
                token = config.token, owner = config.owner, repo = config.repo,
                branch = config.branch, workflowFile = config.workflowFile, isConfigured = true
            )
        } else SettingsUiState()
    }

    fun onFieldChange(update: (SettingsUiState) -> SettingsUiState) {
        uiState.value = update(uiState.value)
    }

    fun save() {
        val s = uiState.value
        if (s.token.isBlank() || s.owner.isBlank() || s.repo.isBlank()) {
            uiState.value = s.copy(savedMessage = "Token, owner, and repo are required")
            return
        }
        configStore.save(GitHubConfig(s.token.trim(), s.owner.trim(), s.repo.trim(), s.branch.trim().ifBlank { "main" }, s.workflowFile.trim().ifBlank { "diabo-preview-build.yml" }))
        uiState.value = s.copy(isConfigured = true, savedMessage = "GitHub connected ✅")
    }

    fun disconnect() {
        configStore.clear()
        uiState.value = SettingsUiState(savedMessage = "Disconnected")
    }

    fun dismissMessage() {
        uiState.value = uiState.value.copy(savedMessage = null)
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            uiState.value = uiState.value.copy(backupMessage = "Exporting…")
            val result = backupRepository.exportAll(uri)
            uiState.value = uiState.value.copy(
                backupMessage = when (result) {
                    is BackupResult.Success -> "Backed up ${result.projectsRestored} project(s) ✅"
                    is BackupResult.Error -> result.message
                }
            )
        }
    }

    fun restoreBackup(uri: Uri) {
        viewModelScope.launch {
            uiState.value = uiState.value.copy(backupMessage = "Restoring…")
            val result = backupRepository.restoreFrom(uri)
            uiState.value = uiState.value.copy(
                backupMessage = when (result) {
                    is BackupResult.Success -> "Restored ${result.projectsRestored} new project(s) ✅"
                    is BackupResult.Error -> result.message
                }
            )
        }
    }

    fun setLanguage(bangla: Boolean) {
        if (bangla) LanguageManager.setBangla() else LanguageManager.setEnglish()
        uiState.value = uiState.value.copy(isBangla = bangla)
    }
}

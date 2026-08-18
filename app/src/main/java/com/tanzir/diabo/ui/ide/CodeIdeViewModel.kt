package com.tanzir.diabo.ui.ide

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanzir.diabo.data.local.entity.FileType
import com.tanzir.diabo.data.local.entity.Project
import com.tanzir.diabo.data.local.entity.ProjectFile
import com.tanzir.diabo.data.repository.DiaBoResult
import com.tanzir.diabo.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OpenTab(
    val file: ProjectFile,
    val content: String,
    val isDirty: Boolean = false
)

sealed class NewEntryTarget { data object File : NewEntryTarget(); data object Folder : NewEntryTarget() }

data class CodeIdeUiState(
    val project: Project? = null,
    val files: List<ProjectFile> = emptyList(),
    val openTabs: List<OpenTab> = emptyList(),
    val activeTabIndex: Int = 0,
    val isSidebarOpen: Boolean = true,
    val newEntryTarget: NewEntryTarget? = null,
    val renameTarget: ProjectFile? = null,
    val deleteTarget: ProjectFile? = null,
    val searchQuery: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val showInstantPreview: Boolean = false
)

private const val AUTOSAVE_DEBOUNCE_MS = 800L

@HiltViewModel
class CodeIdeViewModel @Inject constructor(
    private val repository: ProjectRepository,
    private val cloudBuildRepository: com.tanzir.diabo.data.build.CloudBuildRepository,
    private val workManager: androidx.work.WorkManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val projectId: String = checkNotNull(savedStateHandle["projectId"])

    private val project = MutableStateFlow<Project?>(null)
    private val openTabs = MutableStateFlow<List<OpenTab>>(emptyList())
    private val activeTabIndex = MutableStateFlow(0)
    private val isSidebarOpen = MutableStateFlow(true)
    private val newEntryTarget = MutableStateFlow<NewEntryTarget?>(null)
    private val renameTarget = MutableStateFlow<ProjectFile?>(null)
    private val deleteTarget = MutableStateFlow<ProjectFile?>(null)
    private val searchQuery = MutableStateFlow("")
    private val isSaving = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val showInstantPreview = MutableStateFlow(false)

    private var autosaveJob: Job? = null

    init {
        viewModelScope.launch {
            // In Phase 1 there's no dedicated getById Flow — this keeps the repository
            // surface small; a tiny suspend fetch is enough since project rarely changes underfoot.
            repository.observeProjects().collect { all ->
                project.value = all.firstOrNull { it.id == projectId } ?: project.value
            }
        }
    }

    val files: StateFlow<List<ProjectFile>> =
        repository.observeProjectFiles(projectId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<CodeIdeUiState> = combine(
        project, files, openTabs, activeTabIndex, isSidebarOpen,
        newEntryTarget, renameTarget, deleteTarget, searchQuery, isSaving, errorMessage, showInstantPreview
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        CodeIdeUiState(
            project = values[0] as Project?,
            files = values[1] as List<ProjectFile>,
            openTabs = values[2] as List<OpenTab>,
            activeTabIndex = values[3] as Int,
            isSidebarOpen = values[4] as Boolean,
            newEntryTarget = values[5] as NewEntryTarget?,
            renameTarget = values[6] as ProjectFile?,
            deleteTarget = values[7] as ProjectFile?,
            searchQuery = values[8] as String,
            isSaving = values[9] as Boolean,
            errorMessage = values[10] as String?,
            showInstantPreview = values[11] as Boolean
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CodeIdeUiState())

    fun toggleSidebar() { isSidebarOpen.value = !isSidebarOpen.value }

    fun toggleInstantPreview() { showInstantPreview.value = !showInstantPreview.value }
    fun dismissInstantPreview() { showInstantPreview.value = false }

    // ---- Phase 3: Real Build (Cloud Build via GitHub Actions) ----

    private val currentBuildId = MutableStateFlow<String?>(null)
    private val showRealBuild = MutableStateFlow(false)
    private val triggerError = MutableStateFlow<String?>(null)

    val currentBuild: StateFlow<com.tanzir.diabo.data.local.entity.BuildRecord?> = currentBuildId
        .flatMapLatest { id -> if (id == null) kotlinx.coroutines.flow.flowOf(null) else cloudBuildRepository.observeBuild(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    data class RealBuildUiState(
        val isVisible: Boolean = false,
        val build: com.tanzir.diabo.data.local.entity.BuildRecord? = null,
        val triggerError: String? = null
    )

    val realBuildUiState: StateFlow<RealBuildUiState> = combine(
        showRealBuild, currentBuild, triggerError
    ) { visible, build, error ->
        RealBuildUiState(visible, build, error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RealBuildUiState())

    fun triggerRealBuild() {
        val proj = project.value ?: return
        val xmlContent = activePreviewXml.value
        val javaContent = activePreviewJava.value
        if (xmlContent.isNullOrBlank() || javaContent.isNullOrBlank()) {
            triggerError.value = "Open both a .java and a .xml file before running a Real Build"
            showRealBuild.value = true
            return
        }

        showRealBuild.value = true
        triggerError.value = null

        viewModelScope.launch {
            when (val result = cloudBuildRepository.triggerBuild(proj.id, javaContent, xmlContent)) {
                is com.tanzir.diabo.data.build.TriggerBuildResult.Started -> {
                    currentBuildId.value = result.buildId
                    val request = androidx.work.OneTimeWorkRequestBuilder<com.tanzir.diabo.data.build.BuildPollWorker>()
                        .setInputData(
                            androidx.work.workDataOf(
                                com.tanzir.diabo.data.build.BuildPollWorker.KEY_BUILD_ID to result.buildId,
                                com.tanzir.diabo.data.build.BuildPollWorker.KEY_PROJECT_ID to proj.id
                            )
                        )
                        .setConstraints(
                            androidx.work.Constraints.Builder()
                                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                                .build()
                        )
                        .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .build()
                    workManager.enqueue(request)
                }
                is com.tanzir.diabo.data.build.TriggerBuildResult.NotConfigured -> triggerError.value = result.message
                is com.tanzir.diabo.data.build.TriggerBuildResult.Error -> triggerError.value = result.message
            }
        }
    }

    fun dismissRealBuild() { showRealBuild.value = false }

    /** Content of the most recently active XML/Java tabs — falls back to null if none are open. */
    val activePreviewXml: StateFlow<String?> = openTabs
        .let { flow -> kotlinx.coroutines.flow.combine(flow, activeTabIndex) { tabs, idx ->
            tabs.getOrNull(idx)?.takeIf { it.file.type == FileType.XML }?.content
                ?: tabs.firstOrNull { it.file.type == FileType.XML }?.content
        } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activePreviewJava: StateFlow<String?> = openTabs
        .let { flow -> kotlinx.coroutines.flow.combine(flow, activeTabIndex) { tabs, _ ->
            tabs.firstOrNull { it.file.type == FileType.JAVA }?.content
        } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun openFile(file: ProjectFile) {
        val existingIndex = openTabs.value.indexOfFirst { it.file.id == file.id }
        if (existingIndex >= 0) {
            activeTabIndex.value = existingIndex
            return
        }
        val proj = project.value ?: return
        viewModelScope.launch {
            when (val result = repository.readFileContent(proj, file)) {
                is DiaBoResult.Success -> {
                    openTabs.value = openTabs.value + OpenTab(file, result.data)
                    activeTabIndex.value = openTabs.value.lastIndex
                }
                is DiaBoResult.Error -> errorMessage.value = result.message
            }
        }
    }

    fun closeTab(index: Int) {
        if (index !in openTabs.value.indices) return
        val tab = openTabs.value[index]
        if (tab.isDirty) {
            // Flush pending changes before closing so nothing is lost.
            persist(tab)
        }
        openTabs.value = openTabs.value.toMutableList().also { it.removeAt(index) }
        activeTabIndex.value = activeTabIndex.value.coerceIn(0, (openTabs.value.size - 1).coerceAtLeast(0))
    }

    fun setActiveTab(index: Int) { activeTabIndex.value = index }

    fun onEditorContentChange(newContent: String) {
        val index = activeTabIndex.value
        val tabs = openTabs.value.toMutableList()
        if (index !in tabs.indices) return
        tabs[index] = tabs[index].copy(content = newContent, isDirty = true)
        openTabs.value = tabs
        scheduleAutosave(tabs[index])
    }

    private fun scheduleAutosave(tab: OpenTab) {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(AUTOSAVE_DEBOUNCE_MS)
            persist(tab)
        }
    }

    private fun persist(tab: OpenTab) {
        val proj = project.value ?: return
        viewModelScope.launch {
            isSaving.value = true
            when (val result = repository.saveFileContent(proj, tab.file, tab.content)) {
                is DiaBoResult.Success -> {
                    val idx = openTabs.value.indexOfFirst { it.file.id == tab.file.id }
                    if (idx >= 0) {
                        val tabs = openTabs.value.toMutableList()
                        tabs[idx] = tabs[idx].copy(isDirty = false)
                        openTabs.value = tabs
                    }
                }
                is DiaBoResult.Error -> errorMessage.value = result.message
            }
            isSaving.value = false
        }
    }

    /**
     * Synchronous save used ONLY from [onCleared]. viewModelScope is already cancelled by
     * the time onCleared() runs, so persist()'s `viewModelScope.launch { ... }` would be a
     * silent no-op there — a real data-loss bug for a "type, immediately back out" flow.
     * A brief blocking file write here (typically a few ms) is the safe tradeoff.
     */
    private fun persistBlocking(tab: OpenTab) {
        val proj = project.value ?: return
        kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
            repository.saveFileContent(proj, tab.file, tab.content)
        }
    }

    fun requestNewEntry(target: NewEntryTarget) { newEntryTarget.value = target }
    fun dismissNewEntry() { newEntryTarget.value = null }

    fun createEntry(name: String) {
        val proj = project.value ?: return
        val target = newEntryTarget.value ?: return
        viewModelScope.launch {
            val result = when (target) {
                is NewEntryTarget.File -> repository.createFile(proj, relativeDir = "src", fileName = name)
                is NewEntryTarget.Folder -> {
                    val r = repository.createFolder(proj, relativeDir = "", folderName = name)
                    if (r is DiaBoResult.Error) DiaBoResult.Error(r.message) else null
                }
            }
            if (result is DiaBoResult.Error) errorMessage.value = result.message
            newEntryTarget.value = null
        }
    }

    fun requestRename(file: ProjectFile) { renameTarget.value = file }
    fun dismissRename() { renameTarget.value = null }

    fun confirmRename(newName: String) {
        val proj = project.value ?: return
        val target = renameTarget.value ?: return
        viewModelScope.launch {
            when (val result = repository.renameFile(proj, target, newName)) {
                is DiaBoResult.Error -> errorMessage.value = result.message
                else -> Unit
            }
            renameTarget.value = null
        }
    }

    fun requestDelete(file: ProjectFile) { deleteTarget.value = file }
    fun dismissDelete() { deleteTarget.value = null }

    // ---- Trash (restore within 24h, matches the promise in the delete confirmation) ----

    private val showTrash = MutableStateFlow(false)

    val trashedFiles: StateFlow<List<ProjectFile>> = repository.observeTrash(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleTrash() { showTrash.value = !showTrash.value }
    fun dismissTrash() { showTrash.value = false }
    val isTrashVisible: StateFlow<Boolean> = showTrash

    fun restoreFile(file: ProjectFile) {
        viewModelScope.launch { repository.restoreFile(file) }
    }

    fun confirmDelete() {
        val target = deleteTarget.value ?: return
        viewModelScope.launch {
            repository.softDeleteFile(target)
            openTabs.value = openTabs.value.filterNot { it.file.id == target.id }
            deleteTarget.value = null
        }
    }

    fun duplicateFile(file: ProjectFile) {
        val proj = project.value ?: return
        viewModelScope.launch {
            val result = repository.duplicateFile(proj, file)
            if (result is DiaBoResult.Error) errorMessage.value = result.message
        }
    }

    fun togglePin(file: ProjectFile) {
        viewModelScope.launch { repository.setPinned(file, !file.isPinned) }
    }

    fun onSearchQueryChange(q: String) { searchQuery.value = q }

    fun dismissError() { errorMessage.value = null }

    override fun onCleared() {
        super.onCleared()
        // viewModelScope is already cancelled at this point, so this MUST use the
        // synchronous persistBlocking — see its doc comment for why.
        openTabs.value.filter { it.isDirty }.forEach { persistBlocking(it) }
    }
}

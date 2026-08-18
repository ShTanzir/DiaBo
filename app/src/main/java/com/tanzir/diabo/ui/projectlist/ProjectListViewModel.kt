package com.tanzir.diabo.ui.projectlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanzir.diabo.data.local.entity.Project
import com.tanzir.diabo.data.repository.DiaBoResult
import com.tanzir.diabo.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortOption { NAME, LAST_MODIFIED, CREATED_DATE }
enum class ViewMode { GRID, LIST }

data class ProjectListUiState(
    val projects: List<Project> = emptyList(),
    val query: String = "",
    val sortOption: SortOption = SortOption.LAST_MODIFIED,
    val viewMode: ViewMode = ViewMode.GRID,
    val selectedIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val pendingDeleteProject: Project? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class ProjectListViewModel @Inject constructor(
    private val repository: ProjectRepository
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val sortOption = MutableStateFlow(SortOption.LAST_MODIFIED)
    private val viewMode = MutableStateFlow(ViewMode.GRID)
    private val selectedIds = MutableStateFlow(setOf<String>())
    private val pendingDeleteProject = MutableStateFlow<Project?>(null)
    private val errorMessage = MutableStateFlow<String?>(null)

    private val filteredProjects = query.flatMapLatest { q ->
        if (q.isBlank()) repository.observeProjects() else repository.searchProjects(q)
    }

    val uiState: StateFlow<ProjectListUiState> = combine(
        filteredProjects, query, sortOption, viewMode, selectedIds, pendingDeleteProject, errorMessage
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val projects = values[0] as List<Project>
        val q = values[1] as String
        val sort = values[2] as SortOption
        val vMode = values[3] as ViewMode
        val selected = values[4] as Set<String>
        val pending = values[5] as Project?
        val error = values[6] as String?

        val sorted = when (sort) {
            SortOption.NAME -> projects.sortedBy { it.name.lowercase() }
            SortOption.LAST_MODIFIED -> projects.sortedByDescending { it.lastModified }
            SortOption.CREATED_DATE -> projects.sortedByDescending { it.createdAt }
        }

        ProjectListUiState(
            projects = sorted,
            query = q,
            sortOption = sort,
            viewMode = vMode,
            selectedIds = selected,
            isSelectionMode = selected.isNotEmpty(),
            pendingDeleteProject = pending,
            errorMessage = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProjectListUiState())

    fun onQueryChange(q: String) { query.value = q }
    fun onSortOptionChange(option: SortOption) { sortOption.value = option }
    fun onViewModeToggle() {
        viewMode.value = if (viewMode.value == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID
    }

    fun toggleSelection(projectId: String) {
        selectedIds.value = if (projectId in selectedIds.value) {
            selectedIds.value - projectId
        } else {
            selectedIds.value + projectId
        }
    }

    fun clearSelection() { selectedIds.value = emptySet() }

    fun requestDelete(project: Project) { pendingDeleteProject.value = project }
    fun cancelDelete() { pendingDeleteProject.value = null }

    fun confirmDelete() {
        val project = pendingDeleteProject.value ?: return
        viewModelScope.launch {
            when (val result = repository.deleteProject(project)) {
                is DiaBoResult.Error -> errorMessage.value = result.message
                else -> Unit
            }
            pendingDeleteProject.value = null
        }
    }

    fun duplicateProject(project: Project) {
        viewModelScope.launch {
            val result = repository.duplicateProject(project)
            if (result is DiaBoResult.Error) errorMessage.value = result.message
        }
    }

    fun renameProject(project: Project, newName: String) {
        viewModelScope.launch {
            val result = repository.renameProject(project, newName)
            if (result is DiaBoResult.Error) errorMessage.value = result.message
        }
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val toDelete = uiState.value.projects.filter { it.id in selectedIds.value }
            toDelete.forEach { repository.deleteProject(it) }
            selectedIds.value = emptySet()
        }
    }

    fun dismissError() { errorMessage.value = null }
}

package com.tanzir.diabo.ui.home

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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val recentProjects: List<Project> = emptyList(),
    val totalProjects: Int = 0,
    val isCreatingProject: Boolean = false,
    val createProjectError: String? = null,
    val showNewProjectDialog: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ProjectRepository
) : ViewModel() {

    private val uiExtras = MutableStateFlow(Triple(false, false, null as String?))

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeRecentProjects(5),
        repository.observeProjects(),
        uiExtras
    ) { recent, all, extras ->
        HomeUiState(
            recentProjects = recent,
            totalProjects = all.size,
            showNewProjectDialog = extras.first,
            isCreatingProject = extras.second,
            createProjectError = extras.third
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun onFabClick() {
        uiExtras.value = uiExtras.value.copy(first = true)
    }

    fun dismissNewProjectDialog() {
        uiExtras.value = Triple(false, false, null)
    }

    fun createProject(name: String, template: com.tanzir.diabo.templates.ProjectTemplate?, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            uiExtras.value = uiExtras.value.copy(second = true, third = null)
            when (val result = repository.createProject(name, template)) {
                is DiaBoResult.Success -> {
                    uiExtras.value = Triple(false, false, null)
                    onCreated(result.data.id)
                }
                is DiaBoResult.Error -> {
                    uiExtras.value = uiExtras.value.copy(second = false, third = result.message)
                }
            }
        }
    }
}

private fun Triple<Boolean, Boolean, String?>.copy(
    first: Boolean = this.first,
    second: Boolean = this.second,
    third: String? = this.third
) = Triple(first, second, third)

package com.devmarkabrasaldo.PataGilid.ui.screens.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.devmarkabrasaldo.PataGilid.data.repository.MountainListRepository
import com.devmarkabrasaldo.PataGilid.domain.models.MountainList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.devmarkabrasaldo.PataGilid.data.repository.MountainRepository
import com.devmarkabrasaldo.PataGilid.domain.models.Mountain

data class MountainListsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class MountainListsViewModel(
    private val repository: MountainListRepository,
    private val mountainRepository: MountainRepository
) : ViewModel() {

    // MARK: - State

    val lists: StateFlow<List<MountainList>> = repository.observeLists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allMountains: StateFlow<List<Mountain>> = mountainRepository.allMountainsByName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(MountainListsUiState())
    val uiState: StateFlow<MountainListsUiState> = _uiState

    // MARK: - Init

    init {
        syncFromFirestore()
    }

    // MARK: - Sync

    fun syncFromFirestore() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                repository.syncFromFirestore()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage)
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    // MARK: - Create

    fun createList(name: String, emoji: String, onCreated: (String) -> Unit = {}) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                val id = repository.createList(name, emoji)
                onCreated(id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage)
            }
        }
    }

    // MARK: - Rename

    fun renameList(listId: String, newName: String, newEmoji: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            try {
                repository.renameList(listId, newName, newEmoji)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage)
            }
        }
    }

    // MARK: - Delete

    fun deleteList(list: MountainList) {
        viewModelScope.launch {
            try {
                repository.deleteList(list.id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage)
            }
        }
    }

    // MARK: - Add / Remove Mountain

    fun addMountain(listId: String, mountainId: String) {
        viewModelScope.launch {
            try {
                repository.addMountain(listId, mountainId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage)
            }
        }
    }

    fun removeMountain(listId: String, mountainId: String) {
        viewModelScope.launch {
            try {
                repository.removeMountain(listId, mountainId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    // MARK: - Factory

    class Factory(
        private val repository: MountainListRepository,
        private val mountainRepository: MountainRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MountainListsViewModel(repository, mountainRepository) as T
    }
}

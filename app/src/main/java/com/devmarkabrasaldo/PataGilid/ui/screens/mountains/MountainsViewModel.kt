package com.devmarkabrasaldo.PataGilid.ui.screens.mountains

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.devmarkabrasaldo.PataGilid.data.repository.MountainRepository
import com.devmarkabrasaldo.PataGilid.domain.models.IslandGroup
import com.devmarkabrasaldo.PataGilid.domain.models.Mountain
import com.devmarkabrasaldo.PataGilid.domain.models.RegionHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SortType(val label: String) {
    ELEVATION_HIGH_TO_LOW("Elevation: High to Low"),
    ELEVATION_LOW_TO_HIGH("Elevation: Low to High"),
    NAME_AZ("Name (A-Z)")
}

class MountainsViewModel(private val repository: MountainRepository) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val selectedIslandGroup = MutableStateFlow<IslandGroup?>(null) // null = ALL
    val selectedRegion = MutableStateFlow<String?>(null)
    val sortType = MutableStateFlow(SortType.ELEVATION_HIGH_TO_LOW)
    val isSyncing = MutableStateFlow(false)
    val syncProgress = MutableStateFlow<Float?>(null)

    val allMountains: StateFlow<List<Mountain>> = repository.allMountainsByElevation
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val totalMountainsCount: StateFlow<Int> = allMountains
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val availableRegions: StateFlow<List<String>> = combine(
        allMountains,
        selectedIslandGroup
    ) { list, island ->
        val candidateMountains = if (island != null) list.filter { it.islandGroupEnum == island } else list
        RegionHelper.sortRegions(candidateMountains.map { it.region })
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val mountains: StateFlow<List<Mountain>> = combine(
        allMountains,
        searchQuery,
        selectedIslandGroup,
        selectedRegion,
        sortType
    ) { list, query, island, region, sort ->
        var result = list

        // Island Group filter
        if (island != null) {
            result = result.filter { it.islandGroupEnum == island }
        }

        // Region filter
        if (region != null) {
            result = result.filter { it.region == region }
        }

        // Search query
        if (query.isNotBlank()) {
            result = result.filter { mountain ->
                mountain.name.contains(query, ignoreCase = true) ||
                mountain.region.contains(query, ignoreCase = true) ||
                mountain.descriptionText.contains(query, ignoreCase = true)
            }
        }

        // Sort Order
        when (sort) {
            SortType.ELEVATION_HIGH_TO_LOW -> result.sortedByDescending { it.elevationMASL }
            SortType.ELEVATION_LOW_TO_HIGH -> result.sortedBy { it.elevationMASL }
            SortType.NAME_AZ -> result.sortedBy { it.name.lowercase() }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        triggerSync()
    }

    fun selectIslandGroup(group: IslandGroup?) {
        selectedIslandGroup.value = group
        selectedRegion.value = null
    }

    fun selectRegion(region: String?) {
        selectedRegion.value = region
        if (region != null) {
            val match = allMountains.value.firstOrNull { it.region == region }
            if (match != null) {
                selectedIslandGroup.value = match.islandGroupEnum
            }
        }
    }

    fun resetFilters() {
        selectedIslandGroup.value = null
        selectedRegion.value = null
        searchQuery.value = ""
        sortType.value = SortType.ELEVATION_HIGH_TO_LOW
    }

    fun triggerSync() {
        viewModelScope.launch {
            isSyncing.value = true
            syncProgress.value = null
            try {
                repository.synchronize { applied, total ->
                    if (total > 0) {
                        syncProgress.value = applied.toFloat() / total.toFloat()
                    }
                }
            } finally {
                isSyncing.value = false
                syncProgress.value = null
            }
        }
    }

    class Factory(private val repository: MountainRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MountainsViewModel(repository) as T
        }
    }
}

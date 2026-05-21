package com.subramanya.artha.ui.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subramanya.artha.data.repository.TagRepository
import com.subramanya.artha.domain.model.Tag
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TagsUiState(val tags: List<Tag> = emptyList())

class TagsViewModel(private val tagRepository: TagRepository) : ViewModel() {

    val state: StateFlow<TagsUiState> = tagRepository.observeAll()
        .map { TagsUiState(tags = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TagsUiState())

    suspend fun usageCount(id: String): Int = tagRepository.usageCount(id)

    fun upsert(tag: Tag) {
        viewModelScope.launch { tagRepository.upsert(tag) }
    }

    fun delete(tag: Tag) {
        viewModelScope.launch { tagRepository.delete(tag) }
    }
}

class TagsViewModelFactory(private val tagRepository: TagRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(TagsViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return TagsViewModel(tagRepository) as T
    }
}

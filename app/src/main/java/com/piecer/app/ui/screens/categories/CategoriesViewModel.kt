package com.piecer.app.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecer.app.data.local.entity.CategoryEntity
import com.piecer.app.data.local.entity.CategoryType
import com.piecer.app.data.repository.CategoryRepository
import com.piecer.app.util.CategoryValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CategoryFormError {
    EMPTY_NAME,
    DUPLICATE_NAME
}

data class CategoryFormState(
    val isVisible: Boolean = false,
    val editingId: Long? = null,
    val nameInput: String = "",
    val type: CategoryType = CategoryType.EXPENSE,
    val iconKey: String = "restaurant",
    val colorKey: String = "coral",
    val isDefault: Boolean = false,
    val isArchived: Boolean = false,
    val error: CategoryFormError? = null,
    val isSaving: Boolean = false
)

data class CategoryListItem(
    val category: CategoryEntity,
    val isInUse: Boolean
)

data class CategoriesUiState(
    val selectedTab: CategoryType = CategoryType.EXPENSE,
    val activeItems: List<CategoryListItem> = emptyList(),
    val archivedItems: List<CategoryListItem> = emptyList(),
    val form: CategoryFormState = CategoryFormState(),
    val pendingDelete: CategoryEntity? = null
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val selectedTab = MutableStateFlow(CategoryType.EXPENSE)
    private val formState = MutableStateFlow(CategoryFormState())
    private val pendingDelete = MutableStateFlow<CategoryEntity?>(null)
    private val usageMap = MutableStateFlow<Map<Long, Boolean>>(emptyMap())

    val uiState: StateFlow<CategoriesUiState> = combine(
        selectedTab,
        categoryRepository.observeAll(),
        usageMap,
        formState,
        pendingDelete
    ) { tab, allCategories, inUseMap, form, deleteTarget ->
        val filtered = allCategories.filter { it.type == tab }
        val active = filtered.filter { !it.isArchived }.map {
            CategoryListItem(it, inUseMap[it.id] ?: true)
        }
        val archived = filtered.filter { it.isArchived }.map {
            CategoryListItem(it, inUseMap[it.id] ?: true)
        }
        CategoriesUiState(
            selectedTab = tab,
            activeItems = active,
            archivedItems = archived,
            form = form,
            pendingDelete = deleteTarget
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CategoriesUiState()
    )

    init {
        viewModelScope.launch {
            categoryRepository.observeAll().collect { list ->
                val map = HashMap<Long, Boolean>(list.size)
                for (cat in list) {
                    map[cat.id] = categoryRepository.isCategoryInUse(cat.id)
                }
                usageMap.value = map
            }
        }
    }

    fun selectTab(type: CategoryType) {
        selectedTab.value = type
    }

    fun openAddForm() {
        val tab = selectedTab.value
        formState.value = CategoryFormState(
            isVisible = true,
            type = tab,
            iconKey = if (tab == CategoryType.EXPENSE) "restaurant" else "payments",
            colorKey = if (tab == CategoryType.EXPENSE) "coral" else "mint"
        )
    }

    fun openEditForm(category: CategoryEntity) {
        formState.value = CategoryFormState(
            isVisible = true,
            editingId = category.id,
            nameInput = category.name,
            type = category.type,
            iconKey = category.iconKey,
            colorKey = category.colorKey,
            isDefault = category.isDefault,
            isArchived = category.isArchived
        )
    }

    fun dismissForm() {
        formState.update { it.copy(isVisible = false, error = null, isSaving = false) }
    }

    fun onNameChange(value: String) {
        formState.update { it.copy(nameInput = value, error = null) }
    }

    fun onIconSelect(iconKey: String) {
        formState.update { it.copy(iconKey = iconKey) }
    }

    fun onColorSelect(colorKey: String) {
        formState.update { it.copy(colorKey = colorKey) }
    }

    fun saveCategory(onSuccess: () -> Unit) {
        val current = formState.value
        if (current.isSaving) return
        val trimmed = current.nameInput.trim()
        if (trimmed.isEmpty()) {
            formState.update { it.copy(error = CategoryFormError.EMPTY_NAME) }
            return
        }

        formState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val all = categoryRepository.getAll()
            if (CategoryValidator.isDuplicateName(trimmed, current.type, all, current.editingId)) {
                formState.update { it.copy(isSaving = false, error = CategoryFormError.DUPLICATE_NAME) }
                return@launch
            }
            if (current.editingId != null) {
                categoryRepository.update(
                    CategoryEntity(
                        id = current.editingId,
                        name = trimmed,
                        type = current.type,
                        iconKey = current.iconKey,
                        colorKey = current.colorKey,
                        isDefault = current.isDefault,
                        isArchived = current.isArchived
                    )
                )
            } else {
                categoryRepository.insert(
                    CategoryEntity(
                        name = trimmed,
                        type = current.type,
                        iconKey = current.iconKey,
                        colorKey = current.colorKey,
                        isDefault = false,
                        isArchived = false
                    )
                )
            }
            formState.value = CategoryFormState()
            onSuccess()
        }
    }

    fun archiveCategory(category: CategoryEntity, onArchived: () -> Unit) {
        viewModelScope.launch {
            categoryRepository.setArchived(category.id, true)
            onArchived()
        }
    }

    fun restoreCategory(category: CategoryEntity, onRestored: () -> Unit) {
        viewModelScope.launch {
            categoryRepository.setArchived(category.id, false)
            onRestored()
        }
    }

    fun requestDelete(category: CategoryEntity, onCannotDeleteArchivedInstead: () -> Unit) {
        viewModelScope.launch {
            if (categoryRepository.isCategoryInUse(category.id)) {
                categoryRepository.setArchived(category.id, true)
                onCannotDeleteArchivedInstead()
            } else {
                pendingDelete.value = category
            }
        }
    }

    fun dismissDelete() {
        pendingDelete.value = null
    }

    fun confirmDelete(onDeleted: () -> Unit) {
        val target = pendingDelete.value ?: return
        pendingDelete.value = null
        viewModelScope.launch {
            if (categoryRepository.isCategoryInUse(target.id)) {
                categoryRepository.setArchived(target.id, true)
            } else {
                categoryRepository.delete(target)
            }
            onDeleted()
        }
    }
}

package com.piecer.app.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecer.app.data.local.entity.TransactionEntity
import com.piecer.app.data.local.entity.TransactionType
import com.piecer.app.data.repository.CategoryRepository
import com.piecer.app.data.repository.TransactionRepository
import com.piecer.app.data.repository.WalletRepository
import com.piecer.app.ui.components.TransactionDisplayItem
import com.piecer.app.util.DateUtils
import com.piecer.app.util.DayHeaderLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TransactionFilter {
    ALL,
    INCOME,
    EXPENSE,
    TRANSFER
}

data class TransactionDayGroup(
    val dayStartMillis: Long,
    val headerLabel: DayHeaderLabel,
    val items: List<TransactionDisplayItem>
)

data class TransactionsUiState(
    val searchQuery: String = "",
    val selectedFilter: TransactionFilter = TransactionFilter.ALL,
    val selectedCategoryId: Long? = null,
    val selectedCategoryName: String? = null,
    val groups: List<TransactionDayGroup> = emptyList(),
    val hasAnyTransaction: Boolean = false
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    walletRepository: WalletRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedFilter = MutableStateFlow(TransactionFilter.ALL)
    private val selectedCategory = MutableStateFlow<Pair<Long, String>?>(null)

    private val filterInputsFlow = combine(
        searchQuery,
        selectedFilter,
        selectedCategory
    ) { query, filter, category ->
        Triple(query, filter, category)
    }

    val uiState: StateFlow<TransactionsUiState> = combine(
        transactionRepository.observeAll(),
        categoryRepository.observeAll(),
        walletRepository.observeAll(),
        filterInputsFlow
    ) { transactions, categories, wallets, (query, filter, categoryFilter) ->
        val categoryMap = categories.associateBy { it.id }
        val walletMap = wallets.associateBy { it.id }
        val trimmedQuery = query.trim().lowercase()

        val filtered = transactions.filter { tx ->
            val matchesFilter = when (filter) {
                TransactionFilter.ALL -> true
                TransactionFilter.INCOME -> tx.type == TransactionType.INCOME
                TransactionFilter.EXPENSE -> tx.type == TransactionType.EXPENSE
                TransactionFilter.TRANSFER -> tx.type == TransactionType.TRANSFER
            }
            if (!matchesFilter) return@filter false

            if (categoryFilter != null && tx.categoryId != categoryFilter.first) {
                return@filter false
            }

            if (trimmedQuery.isEmpty()) return@filter true
            val categoryName = tx.categoryId?.let { categoryMap[it]?.name }.orEmpty()
            tx.note.lowercase().contains(trimmedQuery) ||
                categoryName.lowercase().contains(trimmedQuery)
        }

        val now = System.currentTimeMillis()
        val grouped = filtered
            .groupBy { DateUtils.startOfDayMillis(it.date) }
            .entries
            .sortedByDescending { it.key }
            .map { (dayStart, dayTxs) ->
                TransactionDayGroup(
                    dayStartMillis = dayStart,
                    headerLabel = DateUtils.dayHeaderLabel(dayStart, now),
                    items = dayTxs.map { tx ->
                        toDisplayItem(tx, categoryMap.mapValues { it.value.name }, walletMap.mapValues { it.value.name })
                    }
                )
            }

        TransactionsUiState(
            searchQuery = query,
            selectedFilter = filter,
            selectedCategoryId = categoryFilter?.first,
            selectedCategoryName = categoryFilter?.second,
            groups = grouped,
            hasAnyTransaction = transactions.isNotEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = TransactionsUiState()
    )

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onFilterSelected(filter: TransactionFilter) {
        selectedFilter.value = filter
    }

    fun filterByCategory(categoryId: Long, categoryName: String) {
        selectedFilter.value = TransactionFilter.EXPENSE
        selectedCategory.value = categoryId to categoryName
    }

    fun clearCategoryFilter() {
        selectedCategory.value = null
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            transactionRepository.deleteById(id)
        }
    }

    private fun toDisplayItem(
        tx: TransactionEntity,
        categoryNames: Map<Long, String>,
        walletNames: Map<Long, String>
    ): TransactionDisplayItem {
        val walletName = walletNames[tx.walletId].orEmpty()
        return when (tx.type) {
            TransactionType.TRANSFER -> {
                val toWalletName = tx.toWalletId?.let { walletNames[it] }.orEmpty()
                val routeText = if (walletName.isNotEmpty() && toWalletName.isNotEmpty()) {
                    "$walletName → $toWalletName"
                } else {
                    walletName
                }
                val subtitle = listOf(routeText, tx.note).filter { it.isNotBlank() }.joinToString(" • ")
                TransactionDisplayItem(
                    transaction = tx,
                    title = "Transfer",
                    subtitle = subtitle
                )
            }
            TransactionType.INCOME, TransactionType.EXPENSE -> {
                val categoryName = tx.categoryId?.let { categoryNames[it] } ?: "Tanpa kategori"
                val subtitle = listOf(walletName, tx.note).filter { it.isNotBlank() }.joinToString(" • ")
                TransactionDisplayItem(
                    transaction = tx,
                    title = categoryName,
                    subtitle = subtitle
                )
            }
        }
    }
}

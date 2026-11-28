package com.piecer.app.util.quickentry

import com.piecer.app.data.local.entity.CategoryEntity
import com.piecer.app.data.preferences.SettingsRepository
import com.piecer.app.data.repository.CategoryRepository
import com.piecer.app.data.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuickEntryLearningManager @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository
) {
    @Volatile
    var historyKeywordMap: Map<String, Map<String, Int>> = emptyMap()
        private set

    @Volatile
    private var initialized = false

    suspend fun ensureInitialized() {
        if (initialized) return
        refreshHistoryIndex()
        initialized = true
    }

    suspend fun refreshHistoryIndex() {
        val settings = settingsRepository.getSettings()
        if (!settings.quickEntryLearningEnabled) {
            historyKeywordMap = emptyMap()
            return
        }
        val transactions = transactionRepository.observeAll().first()
        val categories = categoryRepository.observeAll().first()
        historyKeywordMap = withContext(Dispatchers.Default) {
            val catsById = categories.associateBy { it.id }
            CategoryResolver.buildHistoryKeywordMap(transactions, catsById)
        }
    }

    suspend fun learnFromSave(
        note: String,
        chosenCategoryId: Long?,
        predictedCategoryId: Long?,
        categories: List<CategoryEntity>,
        walletId: Long?
    ) {
        if (walletId != null) {
            settingsRepository.setLastUsedWalletId(walletId)
        }
        val settings = settingsRepository.getSettings()
        if (!settings.quickEntryLearningEnabled) return

        if (chosenCategoryId != null && chosenCategoryId != predictedCategoryId && note.isNotBlank()) {
            val chosenCat = categories.firstOrNull { it.id == chosenCategoryId }
            val keyword = CategoryResolver.extractPrimaryNoteKeyword(note)
            if (chosenCat != null && keyword.isNotEmpty()) {
                settingsRepository.saveQuickEntryCorrection(keyword, chosenCat.name)
            }
        }
        refreshHistoryIndex()
    }

    suspend fun resetLearning() {
        settingsRepository.clearQuickEntryCorrections()
        historyKeywordMap = emptyMap()
    }
}

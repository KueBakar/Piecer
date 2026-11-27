package com.piecer.app.util

import com.piecer.app.data.local.entity.CategoryEntity
import com.piecer.app.data.local.entity.CategoryType

object CategoryValidator {
    fun isDuplicateName(
        name: String,
        type: CategoryType,
        existing: List<CategoryEntity>,
        excludeId: Long? = null
    ): Boolean {
        val normalized = name.trim()
        if (normalized.isEmpty()) return false
        return existing.any { cat ->
            cat.id != excludeId &&
                cat.type == type &&
                cat.name.trim().equals(normalized, ignoreCase = true)
        }
    }
}

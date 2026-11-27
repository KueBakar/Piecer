package com.piecer.app.util

import com.piecer.app.data.local.entity.CategoryEntity
import com.piecer.app.data.local.entity.CategoryType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryValidatorTest {

    private val existing = listOf(
        CategoryEntity(1L, "Makan", CategoryType.EXPENSE, "restaurant", "coral", true),
        CategoryEntity(2L, "Lainnya", CategoryType.EXPENSE, "more_horiz", "coral", true),
        CategoryEntity(3L, "Lainnya", CategoryType.INCOME, "more_horiz", "mint", true)
    )

    @Test
    fun `isDuplicateName rejects case-insensitive match within same type`() {
        assertTrue(CategoryValidator.isDuplicateName("makan", CategoryType.EXPENSE, existing))
        assertTrue(CategoryValidator.isDuplicateName("  MAKAN  ", CategoryType.EXPENSE, existing))
    }

    @Test
    fun `isDuplicateName allows same name in different type or when editing self`() {
        assertFalse(CategoryValidator.isDuplicateName("Makan", CategoryType.INCOME, existing))
        assertFalse(CategoryValidator.isDuplicateName("Makan", CategoryType.EXPENSE, existing, excludeId = 1L))
        assertFalse(CategoryValidator.isDuplicateName("Kopi", CategoryType.EXPENSE, existing))
    }
}

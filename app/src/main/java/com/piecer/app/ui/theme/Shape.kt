package com.piecer.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val PiecerShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp), // Tombol 16dp (RULES.md)
    large = RoundedCornerShape(24.dp),  // Kartu 24dp (RULES.md)
    extraLarge = RoundedCornerShape(28.dp)
)

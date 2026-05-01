package ru.plumsoftware.focusstudio.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

val FocusShapes = Shapes(
    extraSmall = RoundedCornerShape(FocusDesign.cornerExtraSmall),
    small = RoundedCornerShape(FocusDesign.cornerMedium),
    medium = RoundedCornerShape(FocusDesign.cornerMedium),
    large = RoundedCornerShape(FocusDesign.cornerLarge),
    extraLarge = RoundedCornerShape(FocusDesign.cornerFull)
)
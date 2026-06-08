package ru.plumsoftware.focusstudio.ui.screen.editor.photo.text

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.plumsoftware.focusstudio.R
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.data.PhotoSettings
import ru.plumsoftware.focusstudio.ui.screen.editor.photo.data.TextElement
import ru.plumsoftware.focusstudio.ui.theme.FocusDesign
import ru.plumsoftware.focusstudio.ui.theme.iOSBlue
import java.util.UUID

@Composable
fun TextControlPanel(
    settings: PhotoSettings,
    selectedTextId: String?,
    onUpdate: (PhotoSettings) -> Unit,
    onSelectText: (String) -> Unit,
    onClose: () -> Unit
) {
    val selectedText = settings.texts.find { it.id == selectedTextId }
    val defaultText = stringResource(R.string.text_new_default)

    Box(modifier = Modifier.fillMaxSize()) {
        if (selectedText == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Surface(
                    onClick = {
                        val newId = UUID.randomUUID().toString()
                        val newText = TextElement(
                            id = newId,
                            text = defaultText,
                            position = Offset(0f, 0f),
                            color = Color.White,
                            fontSize = 40f,
                            fontFamily = "Default"
                        )
                        onUpdate(settings.copy(texts = settings.texts + newText))
                        onSelectText(newId)
                    },
                    color = iOSBlue,
                    shape = RoundedCornerShape(FocusDesign.cornerMedium)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.text_add),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            TextEditPanel(
                selectedText = selectedText,
                onUpdate = { updatedElement ->
                    val newList = settings.texts.map {
                        if (it.id == updatedElement.id) updatedElement else it
                    }
                    onUpdate(settings.copy(texts = newList))
                },
                onClose = onClose
            )
        }
    }
}

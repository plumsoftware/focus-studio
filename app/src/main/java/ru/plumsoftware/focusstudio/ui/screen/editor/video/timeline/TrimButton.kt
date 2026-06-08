package ru.plumsoftware.focusstudio.ui.screen.editor.video.timeline

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.plumsoftware.focusstudio.R
import ru.plumsoftware.focusstudio.ui.theme.iOSBlue

@Composable
fun TrimButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier
            .width(200.dp)
            .height(44.dp),
        colors = ButtonDefaults.buttonColors(containerColor = iOSBlue),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = stringResource(R.string.btn_trim).uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

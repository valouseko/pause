package cz.honestlead.mezera.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import cz.honestlead.mezera.R
import cz.honestlead.mezera.ui.theme.BlueStrong
import cz.honestlead.mezera.ui.theme.Ink
import cz.honestlead.mezera.ui.theme.InkSoft
import cz.honestlead.mezera.ui.theme.Surface

@Composable
fun LanguageSettingsDialog(selected: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        title = { Text(stringResource(R.string.settings), color = Ink) },
        text = {
            Column {
                Text(stringResource(R.string.language), color = Ink)
                Spacer(Modifier.height(8.dp))
                Column(Modifier.selectableGroup()) {
                    listOf("en" to "English", "cs" to "Čeština").forEach { (code, label) ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .selectable(selected == code, role = Role.RadioButton, onClick = { onSelect(code) })
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selected == code, onClick = null)
                            Text(label, color = Ink, modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.language_saved), color = InkSoft)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close), color = BlueStrong) }
        }
    )
}

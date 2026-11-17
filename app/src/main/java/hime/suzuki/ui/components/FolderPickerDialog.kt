package hime.suzuki.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import hime.suzuki.R
import java.io.File
import java.util.Locale

@Composable
fun FolderPickerDialog(
    start: String,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    val initial = remember(start) {
        val candidate = File(start)
        when {
            candidate.isDirectory -> candidate
            File("/storage/emulated/0/Music").isDirectory -> File("/storage/emulated/0/Music")
            else -> File("/storage/emulated/0")
        }
    }
    var current by remember { mutableStateOf(initial) }

    val children = remember(current) {
        current.listFiles()
            ?.asSequence()
            ?.filter { it.isDirectory && !it.name.startsWith(".") }
            ?.sortedBy { it.name.lowercase(Locale.getDefault()) }
            ?.toList()
            ?: emptyList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("İndirme klasörü seç") },
        text = {
            Column {
                Text(
                    current.absolutePath,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { current = current.parentFile ?: current }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painterResource(R.drawable.ic_folder),
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Üst klasör", style = MaterialTheme.typography.bodyMedium)
                }
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(children, key = { it.absolutePath }) { dir ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { current = dir }
                                .padding(vertical = 9.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_folder),
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(dir.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onPick(current.absolutePath) }) {
                Text("Bu klasörü seç")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Vazgeç") }
        },
    )
}

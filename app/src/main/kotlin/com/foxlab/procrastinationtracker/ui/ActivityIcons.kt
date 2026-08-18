package com.foxlab.procrastinationtracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foxlab.procrastinationtracker.R
import com.foxlab.procrastinationtracker.core.ActivityIcons

data class ActivityIconItem(
    val key: String,
    val label: String,
    val icon: ImageVector
)

object ActivityIconRegistry {
    val ICONS = listOf(
        ActivityIconItem("work", "Trabalho", Icons.Filled.Work),
        ActivityIconItem("school", "Estudo", Icons.Filled.School),
        ActivityIconItem("coffee", "Procrastinar", Icons.Filled.Coffee),
        ActivityIconItem("focus", "Foco", Icons.Filled.GpsFixed),
        ActivityIconItem("fitness", "Treino", Icons.Filled.FitnessCenter),
        ActivityIconItem("book", "Leitura", Icons.Filled.MenuBook),
        ActivityIconItem("hobby", "Arte / Lazer", Icons.Filled.Palette),
        ActivityIconItem("rest", "Pausa", Icons.Filled.SelfImprovement),
        ActivityIconItem("home", "Casa / Tarefas", Icons.Filled.Home),
        ActivityIconItem("sleep", "Sono / Relax", Icons.Filled.Bedtime),
        ActivityIconItem("food", "Refeição", Icons.Filled.Restaurant),
        ActivityIconItem("star", "Outro", Icons.Filled.Star)
    )

    private val iconMap = ICONS.associateBy { it.key }

    fun getIconByKey(key: String?): ImageVector? = key?.let { iconMap[it]?.icon }

    /**
     * The icon for an activity. *Which* icon is decided by the shared rule in `:core`
     * ([ActivityIcons.keyFor]); this only turns that key into a Material vector.
     */
    fun iconFor(title: String, iconKey: String?): ImageVector =
        getIconByKey(ActivityIcons.keyFor(title, iconKey)) ?: Icons.Filled.Star
}

@Composable
fun IconPickerDialog(
    selectedKey: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tracker_select_icon), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Selecione o ícone para representar esta atividade:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().height(260.dp)
                ) {
                    items(ActivityIconRegistry.ICONS) { item ->
                        val isSelected = item.key == selectedKey
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    onSelect(item.key)
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = item.label,
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.tracker_cancel))
            }
        }
    )
}

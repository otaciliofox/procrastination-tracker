package com.foxlab.procrastinationtracker.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foxlab.procrastinationtracker.R
import com.foxlab.procrastinationtracker.core.toHoursMinutesString
import com.foxlab.procrastinationtracker.trackerdata.DeviceBreakdown
import com.foxlab.procrastinationtracker.trackerdata.ProfileWeeklyBreakdown
import com.foxlab.procrastinationtracker.trackerdata.entity.ProfileType
import com.foxlab.procrastinationtracker.trackerdata.WeekSummary
import com.foxlab.procrastinationtracker.ui.theme.BoardPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerHistoryModal(
    currentWeekSummary: WeekSummary?,
    pastWeeksSummaries: List<WeekSummary>,
    profileBreakdowns: List<ProfileWeeklyBreakdown>,
    watchBreakdown: DeviceBreakdown?,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val dateFormat = remember { SimpleDateFormat("dd/MM", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = BoardPalette.CanvasTop,
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Insights,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.tracker_history_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Fechar")
                }
            }

            HorizontalDivider()

            // Tabs: Visão Geral, the two fixed profiles, then the watch (only when it has time of
            // its own), then the custom profiles. The watch sits before the customs on purpose --
            // "where did this come from" is a question about the setup, not one more profile.
            val fixedProfiles = profileBreakdowns.filter { it.profile.type != ProfileType.CUSTOM }
            val customProfiles = profileBreakdowns.filter { it.profile.type == ProfileType.CUSTOM }
            val tabs = buildList {
                add(stringResource(R.string.tracker_tab_general))
                addAll(fixedProfiles.map { it.profile.title })
                if (watchBreakdown != null) add(stringResource(R.string.tracker_tab_watch))
                addAll(customProfiles.map { it.profile.title })
            }
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Tab Content
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                if (selectedTabIndex == 0) {
                    GeneralOverviewTab(
                        currentWeek = currentWeekSummary,
                        pastWeeks = pastWeeksSummaries,
                        dateFormat = dateFormat
                    )
                } else {
                    val index = selectedTabIndex - 1
                    val watchTabIndex = if (watchBreakdown != null) fixedProfiles.size else -1
                    when {
                        index == watchTabIndex && watchBreakdown != null ->
                            DeviceBreakdownTab(breakdown = watchBreakdown, dateFormat = dateFormat)

                        index < fixedProfiles.size ->
                            ProfileBreakdownTab(breakdown = fixedProfiles[index], dateFormat = dateFormat)

                        else -> {
                            val customIndex = index - fixedProfiles.size - (if (watchBreakdown != null) 1 else 0)
                            customProfiles.getOrNull(customIndex)?.let {
                                ProfileBreakdownTab(breakdown = it, dateFormat = dateFormat)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GeneralOverviewTab(
    currentWeek: WeekSummary?,
    pastWeeks: List<WeekSummary>,
    dateFormat: SimpleDateFormat
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Current Week Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val weekStartStr = currentWeek?.let { dateFormat.format(Date(it.startSundayMillis)) } ?: "--/--"
                    val weekEndStr = currentWeek?.let { dateFormat.format(Date(it.endSaturdayMillis)) } ?: "--/--"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.tracker_current_week),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$weekStartStr a $weekEndStr",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    val activeMillis = currentWeek?.activeMillis ?: 0L
                    val procMillis = currentWeek?.procrastinatingMillis ?: 0L
                    val totalMillis = currentWeek?.totalMillis ?: 0L
                    val procPercent = currentWeek?.procrastinationPercent ?: 0

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatBadgeCard(
                            title = stringResource(R.string.tracker_active),
                            value = activeMillis.toHoursMinutesString(),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        StatBadgeCard(
                            title = stringResource(R.string.tracker_procrastinating),
                            value = procMillis.toHoursMinutesString(),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Progress bar
                    if (totalMillis > 0) {
                        val activePercent = 100 - procPercent
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Produtivo: $activePercent%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Procrastinado: $procPercent%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        val progress = (activeMillis.toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progress },
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    } else {
                        Text(
                            text = "Rastreie atividades esta semana para ver o balanço.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Past Weeks Evolution Section
        item {
            Text(
                text = stringResource(R.string.tracker_past_weeks),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (pastWeeks.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.tracker_no_past_weeks),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(pastWeeks) { week -> WeekRow(week = week, dateFormat = dateFormat) }
        }
    }
}

/** One consolidated week: range, active vs. procrastinating, and the percentage badge. */
@Composable
private fun WeekRow(week: WeekSummary, dateFormat: SimpleDateFormat) {
    val startStr = dateFormat.format(Date(week.startSundayMillis))
    val endStr = dateFormat.format(Date(week.endSaturdayMillis))

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "$startStr a $endStr",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Ativo: ${week.activeMillis.toHoursMinutesString()}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(" · ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "Procrast.: ${week.procrastinatingMillis.toHoursMinutesString()}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(Modifier.height(4.dp))
                OriginRow(phoneMillis = week.phoneMillis, watchMillis = week.watchMillis)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (week.procrastinationPercent <= 20) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.errorContainer
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${week.procrastinationPercent}% proc.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (week.procrastinationPercent <= 20) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun StatBadgeCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun ProfileBreakdownTab(breakdown: ProfileWeeklyBreakdown, dateFormat: SimpleDateFormat) {
    val totalProfileMillis = breakdown.totalMillis
    val slices = breakdown.slicesWithTotals

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total na Semana",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = totalProfileMillis.toHoursMinutesString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (slices.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.tracker_no_profile_history),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            items(slices) { item ->
                val slice = item.slice
                val sliceMillis = item.totalMillis
                val icon = ActivityIconRegistry.iconFor(slice.title, slice.iconKey)
                val sliceColor = activityColor(slice.title, slice.position)

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(sliceColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = slice.title,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = slice.title,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Text(
                                text = sliceMillis.toHoursMinutesString(),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (sliceMillis > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (totalProfileMillis > 0) {
                            Spacer(Modifier.height(8.dp))
                            val progress = (sliceMillis.toFloat() / totalProfileMillis.toFloat()).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { progress },
                                color = sliceColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                        }
                    }
                }
            }
        }

        // This profile's own history, week by week -- the general tab shows every profile mixed.
        item {
            Text(
                text = stringResource(R.string.tracker_past_weeks),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (breakdown.pastWeeks.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.tracker_no_profile_past_weeks),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        } else {
            items(breakdown.pastWeeks) { week -> WeekRow(week = week, dateFormat = dateFormat) }
        }
    }
}

/**
 * Where the time was recorded. The watch keeps counting while the phone charges, so a week that
 * looks thin on one device isn't a week you didn't work -- showing both makes that legible.
 * Hidden entirely when only one device is involved, which is the common case.
 */
@Composable
private fun OriginRow(phoneMillis: Long, watchMillis: Long) {
    if (phoneMillis <= 0L || watchMillis <= 0L) return
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.Smartphone,
            contentDescription = "Celular",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(13.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = phoneMillis.toHoursMinutesString(),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Icon(
            imageVector = Icons.Filled.Watch,
            contentDescription = "Relógio",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(13.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = watchMillis.toHoursMinutesString(),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * One device's own tab: what it recorded this week, per activity, plus its closed weeks. Same
 * shape as a profile tab so the report reads consistently, but answering a different question --
 * this is the wrist's contribution, whatever profile it happened under.
 */
@Composable
private fun DeviceBreakdownTab(breakdown: DeviceBreakdown, dateFormat: SimpleDateFormat) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Watch,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.tracker_week),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = breakdown.totalMillis.toHoursMinutesString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (breakdown.slicesWithTotals.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.tracker_no_device_week),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(breakdown.slicesWithTotals) { entry ->
                val color = activityColor(entry.slice.title, entry.slice.position)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = ActivityIconRegistry.iconFor(entry.slice.title, entry.slice.iconKey),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = entry.slice.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = entry.totalMillis.toHoursMinutesString(),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.tracker_past_weeks),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (breakdown.pastWeeks.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.tracker_no_device_past_weeks),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(breakdown.pastWeeks) { week -> WeekRow(week = week, dateFormat = dateFormat) }
        }
    }
}

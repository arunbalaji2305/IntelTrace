package com.example.inteltrace_v3.presentation.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.inteltrace_v3.data.local.database.entities.AlertEntity
import com.example.inteltrace_v3.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AlertsScreen(
    viewModel: AlertsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val alerts by viewModel.alerts.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    var showFilterMenu by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SystemBackground)
    ) {
        // Header
        GlassyHeader {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = SystemBlue
                )
            }
            
            Text(
                text = "Security Alerts",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = LabelPrimary
            )
            
            Box {
                IconButton(onClick = { showFilterMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Filter",
                        tint = SystemBlue
                    )
                }
                
                DropdownMenu(
                    expanded = showFilterMenu,
                    onDismissRequest = { showFilterMenu = false },
                    modifier = Modifier.background(CardBackground)
                ) {
                    DropdownMenuItem(
                        text = { Text("All Alerts", color = LabelPrimary) },
                        onClick = {
                            viewModel.setFilter(AlertFilterType.ALL)
                            showFilterMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Unread Only", color = LabelPrimary) },
                        onClick = {
                            viewModel.setFilter(AlertFilterType.UNREAD)
                            showFilterMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Critical", color = SystemRed) },
                        onClick = {
                            viewModel.setFilter(AlertFilterType.CRITICAL)
                            showFilterMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("High", color = SystemOrange) },
                        onClick = {
                            viewModel.setFilter(AlertFilterType.HIGH)
                            showFilterMenu = false
                        }
                    )
                }
            }
        }

        if (alerts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppleSpacing.medium),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SystemGreen,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(AppleSpacing.medium))
                    Text(
                        text = "All Clear!",
                        style = MaterialTheme.typography.titleMedium,
                        color = LabelPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "No security alerts at this time",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LabelSecondary,
                        modifier = Modifier.padding(top = AppleSpacing.small)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = AppleSpacing.medium),
                contentPadding = PaddingValues(top = AppleSpacing.medium, bottom = AppleSpacing.xlarge),
                verticalArrangement = Arrangement.spacedBy(AppleSpacing.small)
            ) {
                item {
                    SectionHeader(
                        when (filterType) {
                            AlertFilterType.ALL -> "All Alerts (${alerts.size})"
                            AlertFilterType.UNREAD -> "Unread Alerts (${alerts.size})"
                            AlertFilterType.CRITICAL -> "Critical Alerts (${alerts.size})"
                            AlertFilterType.HIGH -> "High Priority Alerts (${alerts.size})"
                        }
                    )
                }
                
                items(alerts, key = { it.id }) { alert ->
                    AlertItem(
                        alert = alert,
                        onMarkAsRead = { viewModel.markAsRead(alert.id) },
                        onDismiss = { viewModel.dismiss(alert.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun AlertItem(
    alert: AlertEntity,
    onMarkAsRead: () -> Unit,
    onDismiss: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    AppleCard(
        onClick = {
            expanded = !expanded
            if (!alert.isRead) onMarkAsRead()
        }
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Threat Level Indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(getAlertColor(alert.type))
                )
                
                Spacer(modifier = Modifier.width(AppleSpacing.medium))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = alert.title,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (alert.isRead) FontWeight.Normal else FontWeight.Bold
                            ),
                            color = LabelPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!alert.isRead) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(SystemBlue)
                            )
                        }
                    }
                    
                    Text(
                        text = alert.appName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = LabelSecondary
                    )
                    
                    Text(
                        text = formatTimestamp(alert.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = LabelTertiary
                    )
                }
                
                // Threat Score Badge
                Surface(
                    color = getAlertColor(alert.type).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(AppleRadius.small)
                ) {
                    Text(
                        text = alert.type,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = getAlertColor(alert.type)
                    )
                }
            }
            
            // Expanded Details
            if (expanded) {
                Spacer(modifier = Modifier.height(AppleSpacing.medium))
                HorizontalDivider(color = SystemGray5)
                Spacer(modifier = Modifier.height(AppleSpacing.medium))
                
                Text(
                    text = alert.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LabelPrimary
                )
                
                Spacer(modifier = Modifier.height(AppleSpacing.small))
                
                Row {
                    Text(
                        text = "IP: ",
                        style = MaterialTheme.typography.labelMedium,
                        color = LabelSecondary
                    )
                    Text(
                        text = alert.ipAddress,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = LabelPrimary
                    )
                }
                
                Row {
                    Text(
                        text = "Package: ",
                        style = MaterialTheme.typography.labelMedium,
                        color = LabelSecondary
                    )
                    Text(
                        text = alert.packageName,
                        style = MaterialTheme.typography.labelMedium,
                        color = LabelPrimary
                    )
                }
                
                Row {
                    Text(
                        text = "Threat Score: ",
                        style = MaterialTheme.typography.labelMedium,
                        color = LabelSecondary
                    )
                    Text(
                        text = "${alert.threatScore}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = getAlertColor(alert.type)
                    )
                }
                
                Spacer(modifier = Modifier.height(AppleSpacing.medium))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Dismiss", color = SystemGray)
                    }
                }
            }
        }
    }
}

fun getAlertColor(type: String): Color {
    return when (type.uppercase()) {
        "CRITICAL" -> SystemRed
        "HIGH" -> SystemOrange
        "MEDIUM" -> SystemYellow
        "LOW" -> SystemGreen
        else -> SystemBlue
    }
}

fun formatTimestamp(timestamp: Long): String {
    return SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(timestamp))
}

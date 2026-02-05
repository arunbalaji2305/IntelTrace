package com.example.inteltrace_v3.presentation.reports

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.inteltrace_v3.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val stats by viewModel.statistics.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SystemBackground)
    ) {
        // Apple-style Header
        GlassyHeader {
            Text(
                text = "Reports & Analytics",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = LabelPrimary
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AppleSpacing.medium),
            contentPadding = PaddingValues(top = AppleSpacing.large, bottom = AppleSpacing.xlarge),
            verticalArrangement = Arrangement.spacedBy(AppleSpacing.large)
        ) {
            // Statistics Overview
            item {
                SectionHeader("Overview")
            }
            
            item {
                StatisticsCard(stats)
            }
            
            // Export Section
            item {
                SectionHeader("Export Data")
            }
            
            item {
                ExportCard(
                    title = "Connection History",
                    description = "Export all network connections as CSV",
                    icon = Icons.Default.Share,
                    onExport = {
                        scope.launch {
                            val (success, message) = viewModel.exportConnections(context)
                            if (!success) showToast(context, message)
                        }
                    }
                )
            }
            
            item {
                ExportCard(
                    title = "Threat Report",
                    description = "Export detected threats and security alerts",
                    icon = Icons.Default.Warning,
                    onExport = {
                        scope.launch {
                            val (success, message) = viewModel.exportThreats(context)
                            if (!success) showToast(context, message)
                        }
                    }
                )
            }
            
            item {
                ExportCard(
                    title = "Full Report",
                    description = "Generate comprehensive security report",
                    icon = Icons.Default.Info,
                    onExport = {
                        scope.launch {
                            val (success, message) = viewModel.exportFullReport(context)
                            if (!success) showToast(context, message)
                        }
                    }
                )
            }
            
            // Quick Actions - Redesigned to match Apple theme
            item {
                SectionHeader("Quick Actions")
            }
            
            item {
                AppleCard(
                    onClick = {
                        scope.launch {
                            viewModel.clearOldData()
                            showToast(context, "Old data cleared successfully")
                        }
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(SystemRed.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = SystemRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(AppleSpacing.medium))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Clear Old Data",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = LabelPrimary
                            )
                            Text(
                                text = "Remove data older than 30 days",
                                style = MaterialTheme.typography.labelMedium,
                                color = LabelSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = SystemGray3
                        )
                    }
                }
            }
            
            item {
                AppleCard(
                    onClick = {
                        scope.launch {
                            shareReport(context, viewModel)
                        }
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(SystemBlue.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                tint = SystemBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(AppleSpacing.medium))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Share Report",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = LabelPrimary
                            )
                            Text(
                                text = "Share summary via any app",
                                style = MaterialTheme.typography.labelMedium,
                                color = LabelSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = SystemGray3
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatisticsCard(stats: ReportStatistics) {
    AppleCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(AppleSpacing.medium)
        ) {
            StatRow("Total Connections", stats.totalConnections.toString())
            StatRow("Suspicious Activity", stats.suspiciousConnections.toString())
            StatRow("Malicious IPs Detected", stats.maliciousIps.toString())
            StatRow("Total Alerts", stats.totalAlerts.toString())
            StatRow("Data Monitored", formatBytes(stats.totalBytesMonitored))
            StatRow("Monitoring Since", formatDate(stats.firstConnectionTime))
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = LabelSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = LabelPrimary
        )
    }
}

@Composable
fun ExportCard(
    title: String,
    description: String,
    icon: ImageVector,
    onExport: () -> Unit
) {
    AppleCard(onClick = onExport) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppleSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(SystemBlue.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SystemBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = LabelPrimary
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelMedium,
                    color = LabelSecondary
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = SystemGray3
            )
        }
    }
}

private fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

private suspend fun shareReport(context: Context, viewModel: ReportsViewModel) {
    val reportText = viewModel.generateTextReport()
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, reportText)
        putExtra(Intent.EXTRA_SUBJECT, "IntelTrace Security Report")
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share Report"))
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

private fun formatDate(timestamp: Long): String {
    if (timestamp == 0L) return "No data"
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

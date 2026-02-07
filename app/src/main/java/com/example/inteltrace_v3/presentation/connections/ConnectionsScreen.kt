package com.example.inteltrace_v3.presentation.connections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.inteltrace_v3.domain.models.NetworkConnection
import com.example.inteltrace_v3.ui.theme.*

@Composable
fun ConnectionsScreen(
    viewModel: ConnectionsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val connections by viewModel.connections.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    var showFilterMenu by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SystemBackground)
    ) {
        // iOS-style Navigation Bar
        GlassyHeader {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = SystemBlue
                )
            }
            
            Text(
                text = "Network Connections",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = LabelPrimary
            )
            
            Box {
                IconButton(onClick = { showFilterMenu = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
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
                        text = { Text("All Connections", color = LabelPrimary) },
                        onClick = {
                            viewModel.setFilter(FilterType.ALL)
                            showFilterMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Suspicious Only", color = LabelPrimary) },
                        onClick = {
                            viewModel.setFilter(FilterType.SUSPICIOUS)
                            showFilterMenu = false
                        }
                    )
                }
            }
        }

        if (connections.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppleSpacing.medium),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No connections detected",
                        style = MaterialTheme.typography.titleMedium,
                        color = LabelSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (filterType == FilterType.SUSPICIOUS) "No suspicious activity found" else "Try refreshing or changing filters",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LabelTertiary,
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
                verticalArrangement = Arrangement.spacedBy(AppleSpacing.medium)
            ) {
                item {
                    SectionHeader(if (filterType == FilterType.SUSPICIOUS) "Suspicious Activity" else "Recent Activity")
                }
                
                items(connections, key = { it.id }) { connection ->
                    ConnectionItem(connection)
                }
            }
        }
    }
}

@Composable
fun ConnectionItem(connection: NetworkConnection) {
    AppleCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Indicator (Dot)
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(getThreatColor(connection.threatScore))
            )
            
            Spacer(modifier = Modifier.width(AppleSpacing.medium))
            
            Column(modifier = Modifier.weight(1f)) {
                // App Name
                Text(
                    text = connection.appName.ifEmpty { "Unknown App" },
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = LabelPrimary
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                // IP Address
                Text(
                    text = "${connection.destIp}:${connection.destPort}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LabelPrimary // High intensity
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                // Metadata
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = connection.protocolName,
                        style = MaterialTheme.typography.labelMedium,
                        color = LabelSecondary
                    )
                    if (connection.country != null) {
                        Text(
                            text = " • ${connection.country}",
                            style = MaterialTheme.typography.labelMedium,
                            color = LabelSecondary
                        )
                    }
                }
                
                Text(
                    text = connection.timestampFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = LabelTertiary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            
            // Threat Score Badge (only if significant)
            if (connection.threatScore > 0) {
                Spacer(modifier = Modifier.width(AppleSpacing.small))
                Surface(
                    color = getThreatColor(connection.threatScore).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(AppleRadius.small)
                ) {
                    Text(
                        text = "${connection.threatScore}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = getThreatColor(connection.threatScore)
                    )
                }
            }
        }
    }
}

fun getThreatColor(score: Int): Color {
    return when {
        score >= 80 -> SystemRed
        score >= 60 -> SystemOrange
        score >= 40 -> SystemYellow
        score >= 20 -> SystemGreen
        else -> SystemBlue // Neutral/Safe
    }
}

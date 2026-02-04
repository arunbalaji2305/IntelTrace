package com.example.inteltrace_v3.presentation.dashboard

import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.inteltrace_v3.core.vpn.IntelTraceVpnService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToConnections: () -> Unit,
    onNavigateToThreats: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            // Start VPN service
            val intent = Intent(context, IntelTraceVpnService::class.java).apply {
                action = IntelTraceVpnService.ACTION_START_VPN
            }
            context.startService(intent)
            viewModel.toggleVpn()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("IntelTrace") },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // VPN Status Card
            item {
                VpnStatusCard(
                    isActive = uiState.isVpnActive,
                    onToggle = {
                        if (!uiState.isVpnActive) {
                            val vpnIntent = VpnService.prepare(context)
                            if (vpnIntent != null) {
                                vpnPermissionLauncher.launch(vpnIntent)
                            } else {
                                val intent = Intent(context, IntelTraceVpnService::class.java).apply {
                                    action = IntelTraceVpnService.ACTION_START_VPN
                                }
                                context.startService(intent)
                                viewModel.toggleVpn()
                            }
                        } else {
                            val intent = Intent(context, IntelTraceVpnService::class.java).apply {
                                action = IntelTraceVpnService.ACTION_STOP_VPN
                            }
                            context.startService(intent)
                            viewModel.toggleVpn()
                        }
                    }
                )
            }
            
            // Stats Grid
            item {
                Text(
                    text = "Network Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Connections",
                        value = uiState.totalConnections.toString(),
                        icon = Icons.Default.Star,
                        color = MaterialTheme.colorScheme.primary,
                        onClick = onNavigateToConnections
                    )
                    
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Suspicious",
                        value = uiState.suspiciousConnections.toString(),
                        icon = Icons.Default.Warning,
                        color = Color(0xFFFF9800),
                        onClick = onNavigateToThreats
                    )
                }
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Alerts",
                        value = uiState.unreadAlerts.toString(),
                        icon = Icons.Default.Notifications,
                        color = Color(0xFFF44336),
                        onClick = onNavigateToAlerts
                    )
                    
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Malicious IPs",
                        value = uiState.maliciousIpsDetected.toString(),
                        icon = Icons.Default.Close,
                        color = Color(0xFF9C27B0)
                    )
                }
            }
            
            // Quick Actions
            item {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            item {
                QuickActionButton(
                    text = "View All Connections",
                    icon = Icons.AutoMirrored.Filled.List,
                    onClick = onNavigateToConnections
                )
            }
            
            item {
                QuickActionButton(
                    text = "Threat Analysis",
                    icon = Icons.Default.Search,
                    onClick = onNavigateToThreats
                )
            }
        }
    }
}

@Composable
fun VpnStatusCard(
    isActive: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) 
                Color(0xFF4CAF50).copy(alpha = 0.1f) 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "VPN Protection",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isActive) "Active - Monitoring traffic" else "Inactive",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isActive) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Switch(
                    checked = isActive,
                    onCheckedChange = { onToggle() }
                )
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier,
        onClick = { onClick?.invoke() },
        enabled = onClick != null
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun QuickActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp)
    ) {
        Icon(icon, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text)
    }
}

package com.example.inteltrace_v3.presentation.dashboard

import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.inteltrace_v3.core.vpn.IntelTraceVpnService
import com.example.inteltrace_v3.ui.theme.*

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
    
    // Apple Design Layout: Full screen background with content
    
    // Animated rotation for refresh icon
    val infiniteTransition = rememberInfiniteTransition(label = "refresh")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SystemBackground)
    ) {
        // Glassy Header
        GlassyHeader {
            Text(
                text = "IntelTrace",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = LabelPrimary
            )
            Row {
                IconButton(onClick = { viewModel.refresh() }) {
                    Icon(
                        Icons.Default.Refresh,
                        "Refresh",
                        tint = SystemBlue,
                        modifier = if (uiState.isRefreshing) Modifier.rotate(rotation) else Modifier
                    )
                }
                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Default.Settings, "Settings", tint = SystemBlue)
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AppleSpacing.medium), // 16dp
            contentPadding = PaddingValues(top = AppleSpacing.large, bottom = AppleSpacing.xlarge),
            verticalArrangement = Arrangement.spacedBy(AppleSpacing.large)
        ) {
            // Large Title imitation if not in header
            /* 
            item {
                Text(
                    text = "Dashboard",
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.padding(bottom = AppleSpacing.small)
                )
            }
            */

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
                SectionHeader("Network Statistics")
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppleSpacing.medium)
                ) {
                    AppleStatCard(
                        modifier = Modifier.weight(1f),
                        title = "Connections",
                        value = uiState.totalConnections.toString(),
                        icon = Icons.Default.Star,
                        iconColor = SystemBlue,
                        onClick = onNavigateToConnections
                    )
                    
                    AppleStatCard(
                        modifier = Modifier.weight(1f),
                        title = "Suspicious",
                        value = uiState.suspiciousConnections.toString(),
                        icon = Icons.Default.Warning,
                        iconColor = SystemOrange,
                        onClick = onNavigateToThreats
                    )
                }
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppleSpacing.medium)
                ) {
                    AppleStatCard(
                        modifier = Modifier.weight(1f),
                        title = "Alerts",
                        value = uiState.unreadAlerts.toString(),
                        icon = Icons.Default.Notifications,
                        iconColor = SystemRed,
                        onClick = onNavigateToAlerts
                    )
                    
                    AppleStatCard(
                        modifier = Modifier.weight(1f),
                        title = "Malicious IPs",
                        value = uiState.maliciousIpsDetected.toString(),
                        icon = Icons.Default.Close,
                        iconColor = SystemPurple
                    )
                }
            }
            
            // Quick Actions
            item {
                SectionHeader("Quick Actions")
            }
            
            item {
                AppleQuickActionButton(
                    text = "View All Connections",
                    icon = Icons.AutoMirrored.Filled.List,
                    onClick = onNavigateToConnections
                )
            }
            
            item {
                AppleQuickActionButton(
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
    AppleCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "VPN Protection",
                    style = MaterialTheme.typography.headlineMedium,
                    color = LabelPrimary
                )
                Spacer(modifier = Modifier.height(AppleSpacing.xsmall))
                Text(
                    text = if (isActive) "Active - Monitoring traffic" else "Inactive",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isActive) SystemGreen else LabelSecondary
                )
            }
            
            Switch(
                checked = isActive,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = SystemGreen,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = SystemGray4
                )
            )
        }
    }
}

@Composable
fun AppleStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: (() -> Unit)? = null
) {
    AppleCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start 
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(AppleSpacing.medium))
            Text(
                text = value,
                style = MaterialTheme.typography.displaySmall, // Large geometric number
                color = LabelPrimary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = LabelSecondary
            )
        }
    }
}

@Composable
fun AppleQuickActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    AppleCard(
        onClick = onClick,
        contentPadding = AppleSpacing.medium
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
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
            Spacer(modifier = Modifier.width(AppleSpacing.medium))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = LabelPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = SystemGray3
            )
        }
    }
}


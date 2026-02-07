package com.example.inteltrace_v3.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.inteltrace_v3.ui.theme.*

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
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
                text = "Settings",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = LabelPrimary
            )
            
            // Placeholder for alignment
            Spacer(modifier = Modifier.width(48.dp))
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AppleSpacing.medium),
            contentPadding = PaddingValues(top = AppleSpacing.large, bottom = AppleSpacing.xlarge),
            verticalArrangement = Arrangement.spacedBy(AppleSpacing.small)
        ) {
            // Detection Settings
            item { SectionHeader("Detection Settings") }
            
            item {
                SettingsSliderItem(
                    title = "Threat Threshold",
                    description = "Minimum score to trigger alerts (${uiState.threatThreshold})",
                    value = uiState.threatThreshold.toFloat(),
                    onValueChange = { viewModel.updateThreatThreshold(it.toInt()) },
                    valueRange = 20f..90f
                )
            }
            
            item {
                SettingsToggleItem(
                    title = "Auto Block",
                    description = "Automatically block high-risk connections",
                    icon = Icons.Default.Lock,
                    isChecked = uiState.isAutoBlockEnabled,
                    onToggle = { viewModel.toggleAutoBlock() }
                )
            }
            
            // Notification Settings
            item { SectionHeader("Notifications") }
            
            item {
                SettingsToggleItem(
                    title = "Enable Notifications",
                    description = "Receive alerts for detected threats",
                    icon = Icons.Default.Notifications,
                    isChecked = uiState.notificationsEnabled,
                    onToggle = { viewModel.toggleNotifications() }
                )
            }
            
            item {
                SettingsToggleItem(
                    title = "Critical Alerts Only",
                    description = "Only notify for critical threats",
                    icon = Icons.Default.Warning,
                    isChecked = uiState.criticalAlertsOnly,
                    onToggle = { viewModel.toggleCriticalAlertsOnly() },
                    enabled = uiState.notificationsEnabled
                )
            }
            
            // API Keys
            item { SectionHeader("API Configuration") }
            
            item {
                ApiKeyInput(
                    title = "AbuseIPDB API Key",
                    value = uiState.abuseIPDBApiKey,
                    onValueChange = { viewModel.updateAbuseIPDBApiKey(it) },
                    placeholder = "Enter your AbuseIPDB API key"
                )
            }
            
            item {
                ApiKeyInput(
                    title = "VirusTotal API Key",
                    value = uiState.virusTotalApiKey,
                    onValueChange = { viewModel.updateVirusTotalApiKey(it) },
                    placeholder = "Enter your VirusTotal API key"
                )
            }
            
            item {
                ApiKeyInput(
                    title = "AlienVault OTX API Key",
                    value = uiState.alienVaultApiKey,
                    onValueChange = { viewModel.updateAlienVaultApiKey(it) },
                    placeholder = "Enter your AlienVault OTX API key"
                )
            }
            
            // Data Management
            item { SectionHeader("Data Management") }
            
            item {
                SettingsSliderItem(
                    title = "Data Retention",
                    description = "Keep data for ${uiState.dataRetentionDays} days",
                    value = uiState.dataRetentionDays.toFloat(),
                    onValueChange = { viewModel.updateDataRetention(it.toInt()) },
                    valueRange = 1f..90f
                )
            }
            
            item {
                SettingsActionItem(
                    title = "Clear All Data",
                    description = "Delete all connections, threats, and alerts",
                    icon = Icons.Default.Delete,
                    iconTint = SystemRed,
                    onClick = { viewModel.clearAllData() }
                )
            }
            
            // About
            item { SectionHeader("About") }
            
            item {
                AppleCard {
                    Column {
                        Text(
                            text = "IntelTrace",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = LabelPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Version 1.0.0",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LabelSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Mobile Threat Detection using OSINT & Advanced Heuristics",
                            style = MaterialTheme.typography.bodySmall,
                            color = LabelTertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsToggleItem(
    title: String,
    description: String,
    icon: ImageVector,
    isChecked: Boolean,
    onToggle: () -> Unit,
    enabled: Boolean = true
) {
    AppleCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) SystemBlue else SystemGray,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(AppleSpacing.medium))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = if (enabled) LabelPrimary else LabelTertiary
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) LabelSecondary else LabelTertiary
                )
            }
            
            Switch(
                checked = isChecked,
                onCheckedChange = { onToggle() },
                enabled = enabled,
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
fun SettingsSliderItem(
    title: String,
    description: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>
) {
    AppleCard {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = LabelPrimary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = LabelSecondary
            )
            Spacer(modifier = Modifier.height(AppleSpacing.small))
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                colors = SliderDefaults.colors(
                    thumbColor = SystemBlue,
                    activeTrackColor = SystemBlue,
                    inactiveTrackColor = SystemGray4
                )
            )
        }
    }
}

@Composable
fun SettingsActionItem(
    title: String,
    description: String,
    icon: ImageVector,
    iconTint: Color = SystemBlue,
    onClick: () -> Unit
) {
    AppleCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(AppleSpacing.medium))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = LabelPrimary
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = LabelSecondary
                )
            }
        }
    }
}

@Composable
fun ApiKeyInput(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    var isVisible by remember { mutableStateOf(false) }
    
    AppleCard {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = LabelPrimary
            )
            Spacer(modifier = Modifier.height(AppleSpacing.small))
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(placeholder, color = LabelTertiary) },
                visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { isVisible = !isVisible }) {
                        Icon(
                            imageVector = if (isVisible) Icons.Default.Close else Icons.Default.Edit,
                            contentDescription = if (isVisible) "Hide" else "Show",
                            tint = SystemGray
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SystemBlue,
                    unfocusedBorderColor = SystemGray4,
                    focusedTextColor = LabelPrimary,
                    unfocusedTextColor = LabelPrimary
                ),
                singleLine = true
            )
            if (value.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "✓ API key configured",
                    style = MaterialTheme.typography.labelSmall,
                    color = SystemGreen
                )
            }
        }
    }
}

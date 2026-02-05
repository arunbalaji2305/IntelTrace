package com.example.inteltrace_v3.presentation.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.inteltrace_v3.BuildConfig
import com.example.inteltrace_v3.ui.theme.*

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    var showPrivacyDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SystemBackground)
    ) {
        // Apple-style Header
        GlassyHeader {
            Text(
                text = "About",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = LabelPrimary
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AppleSpacing.medium),
            contentPadding = PaddingValues(top = AppleSpacing.large, bottom = AppleSpacing.xlarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppleSpacing.large)
        ) {
            // App Logo and Info
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(SystemBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(50.dp),
                            tint = SystemBlue
                        )
                    }
                    
                    Text(
                        text = "IntelTrace",
                        style = MaterialTheme.typography.displaySmall,
                        color = LabelPrimary
                    )
                    
                    Text(
                        text = "Network Security Monitor",
                        style = MaterialTheme.typography.bodyLarge,
                        color = LabelSecondary
                    )
                    
                    Text(
                        text = "Version ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.labelMedium,
                        color = LabelTertiary
                    )
                }
            }
            
            // Description
            item {
                AppleCard {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(AppleSpacing.small)
                    ) {
                        Text(
                            text = "About IntelTrace",
                            style = MaterialTheme.typography.headlineMedium,
                            color = LabelPrimary
                        )
                        Text(
                            text = "IntelTrace is a powerful network security monitoring application that helps protect your Android device by analyzing network traffic, detecting suspicious connections, and identifying potential threats using OSINT (Open Source Intelligence).",
                            style = MaterialTheme.typography.bodyLarge,
                            color = LabelSecondary
                        )
                    }
                }
            }
            
            // Features
            item {
                AppleCard {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(AppleSpacing.medium)
                    ) {
                        Text(
                            text = "Key Features",
                            style = MaterialTheme.typography.headlineMedium,
                            color = LabelPrimary
                        )
                        
                        FeatureItem(
                            icon = Icons.Default.CheckCircle,
                            text = "Real-time network monitoring"
                        )
                        FeatureItem(
                            icon = Icons.Default.CheckCircle,
                            text = "Threat detection using OSINT"
                        )
                        FeatureItem(
                            icon = Icons.Default.CheckCircle,
                            text = "VPN-based packet capture (no root)"
                        )
                        FeatureItem(
                            icon = Icons.Default.CheckCircle,
                            text = "Export reports & analytics"
                        )
                        FeatureItem(
                            icon = Icons.Default.CheckCircle,
                            text = "Privacy-first architecture"
                        )
                    }
                }
            }
            
            // Info Cards
            item {
                InfoCard(
                    title = "Privacy Policy",
                    description = "All data is stored locally on your device",
                    icon = Icons.Default.Lock,
                    onClick = { showPrivacyDialog = true }
                )
            }
            
            item {
                InfoCard(
                    title = "Open Source",
                    description = "Built with modern Android technologies",
                    icon = Icons.Default.Info,
                    onClick = { /* Show tech stack dialog */ }
                )
            }
            
            item {
                InfoCard(
                    title = "Help & Support",
                    description = "Documentation and troubleshooting",
                    icon = Icons.Default.Face,
                    onClick = { /* Open help */ }
                )
            }
            
            // Legal
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    Text(
                        text = "Developed for educational purposes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "© 2026 IntelTrace",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
    
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Policy") },
            text = {
                Text(
                    """
                    IntelTrace is designed with privacy in mind:
                    
                    • All data is stored locally on your device
                    • No data is sent to external servers
                    • No personal information is collected
                    • No analytics or tracking
                    • Optional OSINT queries can be disabled
                    • You have full control over your data
                    
                    Only network metadata (IPs, ports) is analyzed, never the actual content of your communications.
                    """.trimIndent()
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Got it")
                }
            }
        )
    }
}

@Composable
fun FeatureItem(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppleSpacing.small)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SystemBlue,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = LabelPrimary
        )
    }
}

@Composable
fun InfoCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    AppleCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppleSpacing.medium),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
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
                Column {
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
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = SystemGray3
            )
        }
    }
}

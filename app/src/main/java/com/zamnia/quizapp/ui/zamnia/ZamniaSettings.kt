package com.zamnia.quizapp.ui.zamnia

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zamnia.quizapp.data.model.Theme
import com.zamnia.quizapp.ui.settings.SettingsViewModel
import com.zamnia.quizapp.ui.zamnia.components.ZamniaBottomNavigation
import com.zamnia.quizapp.ui.zamnia.components.ZamniaHeader

@Composable
fun ZamniaSettingsScreen(
    onNavigateToHub: () -> Unit,
    onNavigateToWallet: () -> Unit,
    onNavigateToPacks: () -> Unit,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val availableThemes by viewModel.availableThemes.collectAsState()

    Scaffold(
        topBar = { ZamniaHeader(coins = userProfile?.coinBalance ?: 0L, onProfileClick = {}) },
        bottomBar = { 
            ZamniaBottomNavigation(
                currentRoute = "settings",
                onNavigateToHub = onNavigateToHub,
                onNavigateToWallet = onNavigateToWallet,
                onNavigateToSettings = {},
                onNavigateToPacks = onNavigateToPacks
            ) 
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                ProfileHeaderCard(
                    name = userProfile?.displayName ?: "Explorer",
                    email = userProfile?.email ?: "",
                    publicId = userProfile?.userId ?: "------"
                )
            }
            
            item {
                ThemeStoreSection(
                    availableThemes = availableThemes,
                    unlockedThemes = userProfile?.unlockedThemes ?: listOf("default"),
                    activeThemeId = userProfile?.activeThemeId ?: "default",
                    onPurchase = { id, price -> viewModel.purchaseTheme(id, price) }
                )
            }
            
            item {
                SystemConfigSection()
            }
            
            item {
                Button(
                    onClick = { 
                        viewModel.logout()
                        onLogout()
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)),
                    shape = CircleShape
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Secure Disconnect", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun ProfileHeaderCard(name: String, email: String, publicId: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary)
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onTertiary)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(text = email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = CircleShape,
                onClick = { }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "ID: $publicId", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun ThemeStoreSection(
    availableThemes: List<Theme>,
    unlockedThemes: List<String>,
    activeThemeId: String,
    onPurchase: (String, Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Visual Core", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = CircleShape
            ) {
                Text(
                    text = "New Artifacts",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(availableThemes) { theme ->
                val isUnlocked = unlockedThemes.contains(theme.id)
                val isActive = activeThemeId == theme.id
                ThemeCard(
                    theme = theme,
                    isUnlocked = isUnlocked,
                    isActive = isActive,
                    onAction = {
                        if (isUnlocked) {
                            // Logic to set active theme
                        } else {
                            onPurchase(theme.id, theme.price.toLong())
                        }
                    }
                )
            }
            
            // Default placeholder if empty
            if (availableThemes.isEmpty()) {
                item {
                    ThemeCard(
                        theme = Theme("default", "Neon Night", 0, "", "#D0BCFF", "#4CD7F6"),
                        isUnlocked = true,
                        isActive = activeThemeId == "default",
                        onAction = {}
                    )
                }
            }
        }
    }
}

@Composable
fun ThemeCard(theme: Theme, isUnlocked: Boolean, isActive: Boolean, onAction: () -> Unit) {
    Card(
        modifier = Modifier.width(260.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isActive) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = theme.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = if (isActive) "Active Protocol" else if (isUnlocked) "Unlocked" else "Available", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (isActive) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.background, CircleShape).border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape))
                Box(modifier = Modifier.size(32.dp).background(Color(android.graphics.Color.parseColor(theme.primaryColor.ifEmpty { "#D0BCFF" })), CircleShape))
                Box(modifier = Modifier.size(32.dp).background(Color(android.graphics.Color.parseColor(theme.secondaryColor.ifEmpty { "#4CD7F6" })), CircleShape))
            }
            Button(
                onClick = onAction,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isActive) MaterialTheme.colorScheme.primary else if (isUnlocked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                ),
                shape = CircleShape,
                enabled = !isActive
            ) {
                val buttonText = if (isActive) "Applied" else if (isUnlocked) "Apply" else "Unlock • ${theme.price}"
                Text(text = buttonText)
                if (!isUnlocked && !isActive) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.MonetizationOn, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
fun SystemConfigSection() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = "System Config", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column {
                SettingsToggleItem(
                    icon = Icons.Default.Vibration,
                    title = "Haptic Feedback",
                    desc = "Tactile response on actions",
                    color = MaterialTheme.colorScheme.primary,
                    checked = true
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                SettingsToggleItem(
                    icon = Icons.Default.NotificationsActive,
                    title = "Push Alerts",
                    desc = "Challenge & reward pings",
                    color = MaterialTheme.colorScheme.secondary,
                    checked = true
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                SettingsClickItem(
                    icon = Icons.Default.CleaningServices,
                    title = "Clear Local Cache",
                    desc = "Free up 124 MB of space",
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
fun SettingsToggleItem(icon: ImageVector, title: String, desc: String, color: Color, checked: Boolean) {
    Row(
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(modifier = Modifier.size(40.dp).background(color.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(text = desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = { }, colors = SwitchDefaults.colors(checkedTrackColor = color))
    }
}

@Composable
fun SettingsClickItem(icon: ImageVector, title: String, desc: String, color: Color) {
    Row(
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(modifier = Modifier.size(40.dp).background(color.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(text = desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

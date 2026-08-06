package com.zamnia.quizapp.ui.zamnia

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zamnia.quizapp.ui.dashboard.DashboardViewModel
import com.zamnia.quizapp.ui.packs.PacksViewModel
import com.zamnia.quizapp.ui.zamnia.components.ZamniaBottomNavigation

data class ActiveSubject(
    val name: String,
    val lastChapter: String,
    val progress: Float,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZamniaDashboardScreen(
    onNavigateToWallet: () -> Unit,
    onNavigateToQuiz: (String?) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPacks: () -> Unit,
    onLogout: () -> Unit,
    viewModel: DashboardViewModel = viewModel(),
    packsViewModel: PacksViewModel = viewModel(),
    authViewModel: com.zamnia.quizapp.ui.auth.AuthViewModel = viewModel()
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val downloadedPacks by packsViewModel.downloadedPackages.collectAsState()
    val activeSubjects by viewModel.activeSubjects.collectAsState()
    val authState by authViewModel.authState.collectAsState()

    val pagerState = rememberPagerState(pageCount = { activeSubjects.size })

    // Use a flag to ensure onLogout is only called once per session loss
    var isLoggingOut by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(userProfile, isLoading, authState) {
        // Strict guard: Only navigate to auth if we are 100% sure the user is unauthenticated
        // and we are not in the middle of a loading operation.
        val isUnauthenticated = userProfile == null && !isLoading && authState is com.zamnia.quizapp.ui.auth.AuthState.LoggedOut
        
        if (isUnauthenticated && !isLoggingOut) {
            isLoggingOut = true
            onLogout()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hub,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "Zamnia",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.8f)
                )
            )
        },
        bottomBar = { 
            ZamniaBottomNavigation(
                currentRoute = "hub",
                onNavigateToWallet = onNavigateToWallet,
                onNavigateToHub = {},
                onNavigateToSettings = onNavigateToSettings,
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
                Spacer(modifier = Modifier.height(8.dp))
                WelcomeSection(name = userProfile?.displayName ?: "Explorer")
            }
            
            item {
                if (activeSubjects.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "RESUME MISSIONS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 2.sp
                        )
                        
                        HorizontalPager(
                            state = pagerState,
                            contentPadding = PaddingValues(end = 32.dp),
                            pageSpacing = 16.dp
                        ) { page ->
                            val subject = activeSubjects[page]
                            ActiveCourseCard(
                                title = subject.name,
                                level = "Recent: ${subject.lastChapter}",
                                progress = subject.progress,
                                color = subject.color,
                                onClick = { onNavigateToQuiz(null) }
                            )
                        }
                    }
                }
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Filter subjects that actually exist in available packs (online or offline)
                    val discoveredSubjects = activeSubjects.map { it.name }.distinct()
                    
                    discoveredSubjects.take(2).forEach { subjectName ->
                        val isDownloaded = activeSubjects.any { it.name == subjectName }
                        val color = when(subjectName.lowercase()) {
                            "physics" -> Color(0xFF4CAF50)
                            "biology" -> Color(0xFFE91E63)
                            "mathematics", "math" -> Color(0xFFF44336)
                            "chemistry" -> Color(0xFFFF9800)
                            else -> MaterialTheme.colorScheme.primary
                        }

                        val title = when(subjectName.lowercase()) {
                            "physics" -> "1st Year Physics"
                            "biology" -> "MDCAT Biology"
                            "mathematics", "math" -> "Mathematics"
                            "chemistry" -> "Chemistry"
                            else -> subjectName
                        }

                        SmallCourseCard(
                            title = title,
                            icon = when(subjectName.lowercase()) {
                                "physics" -> Icons.Default.Book
                                "biology", "science" -> Icons.Default.Science
                                "mathematics", "math" -> Icons.Default.Functions
                                else -> Icons.Default.School
                            },
                            color = color,
                            modifier = Modifier.weight(1f),
                            isDownloaded = isDownloaded,
                            onClick = { 
                                if (isDownloaded) {
                                    val pkgId = downloadedPacks.firstOrNull { it.subject == subjectName }?.packageId
                                    onNavigateToQuiz(pkgId)
                                } else {
                                    onNavigateToPacks()
                                }
                            }
                        )
                    }
                }
            }
            
            item {
                DailyChallengeBanner(onClick = { onNavigateToQuiz(null) })
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun WelcomeSection(name: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = "Welcome back, $name!",
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.ExtraBold,
                brush = Brush.linearGradient(
                    colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                )
            )
        )
        Text(
            text = "Ready to crush your goals today?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ActiveCourseCard(title: String, level: String, progress: Float, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.12f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    color = color.copy(alpha = 0.2f),
                    shape = CircleShape
                ) {
                    Text(
                        text = title.uppercase(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = level,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(containerColor = color),
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Resume", style = MaterialTheme.typography.labelLarge)
                }
            }
            
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(75.dp),
                    strokeWidth = 6.dp,
                    color = color,
                    trackColor = color.copy(alpha = 0.1f)
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (progress > 0f) color else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SmallCourseCard(
    title: String, 
    icon: ImageVector, 
    color: Color, 
    modifier: Modifier = Modifier, 
    isDownloaded: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.height(160.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                lineHeight = 22.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isDownloaded) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth().height(32.dp).clickable { onClick() }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ready", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = onClick,
                        modifier = Modifier.fillMaxWidth().height(32.dp),
                        contentPadding = PaddingValues(0.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f)),
                        shape = CircleShape
                    ) {
                        Text("Get", style = MaterialTheme.typography.labelSmall, color = color)
                    }
                }
            }
        }
    }
}

@Composable
fun DailyChallengeBanner(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DAILY CHALLENGE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Beat the Clock",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(
                onClick = onClick,
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F131D)
@Composable
fun DashboardPreview() {
    com.zamnia.quizapp.ui.theme.ZamniaTheme {
        ZamniaDashboardScreen(
            onNavigateToWallet = {},
            onNavigateToQuiz = { _ -> },
            onNavigateToSettings = {},
            onNavigateToPacks = {},
            onLogout = {}
        )
    }
}

package com.zamnia.quizapp.ui.zamnia

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zamnia.quizapp.ui.packs.PacksNavigationLevel
import com.zamnia.quizapp.ui.packs.PacksViewModel
import com.zamnia.quizapp.ui.zamnia.components.ZamniaBottomNavigation
import com.zamnia.quizapp.ui.zamnia.components.ZamniaHeader

sealed class PackStatus {
    object Offline : PackStatus()
    object Download : PackStatus()
    data class Downloading(val progress: Int) : PackStatus()
    object Locked : PackStatus()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZamniaPacksScreen(
    onNavigateToHub: () -> Unit,
    onNavigateToWallet: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToQuiz: (String) -> Unit,
    viewModel: PacksViewModel = viewModel()
) {
    val navLevel by viewModel.navLevel.collectAsState()
    val selectedClass by viewModel.selectedClass.collectAsState()
    val selectedSubject by viewModel.selectedSubject.collectAsState()
    val availablePacks by viewModel.availablePacks.collectAsState()
    val downloadedPacks by viewModel.downloadedPackages.collectAsState()
    val isDownloadingMap by viewModel.isDownloading.collectAsState()

    // Handle system back button
    BackHandler(enabled = navLevel != PacksNavigationLevel.CLASSES) {
        viewModel.goBack()
    }

    Scaffold(
        topBar = {
            if (navLevel == PacksNavigationLevel.CLASSES) {
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
            } else {
                TopAppBar(
                    title = { 
                        Text(
                            text = when(navLevel) {
                                PacksNavigationLevel.SUBJECTS -> "Class $selectedClass Subjects"
                                PacksNavigationLevel.CHAPTERS -> "$selectedSubject Chapters"
                                else -> "Packs"
                            },
                            style = MaterialTheme.typography.titleMedium
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.goBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    }
                )
            }
        },
        bottomBar = {
            ZamniaBottomNavigation(
                currentRoute = "packs",
                onNavigateToHub = onNavigateToHub,
                onNavigateToWallet = onNavigateToWallet,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToPacks = {}
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            // Merge online packs with local downloaded ones to ensure something is always shown
            val displayPacks = remember(availablePacks, downloadedPacks) {
                if (availablePacks.isEmpty()) {
                    // Create minimal Pack objects from downloaded entities if offline
                    downloadedPacks.map { entity ->
                        com.zamnia.quizapp.data.model.Pack(
                            id = entity.packageId,
                            title = entity.chapterName,
                            subject = entity.subject,
                            classLevel = entity.classLevel,
                            questionCount = "${entity.totalMcqs} Qs",
                            colorHex = "#D0BCFF",
                            iconName = "Book"
                        )
                    }
                } else {
                    availablePacks
                }
            }

            when (navLevel) {
                PacksNavigationLevel.CLASSES -> {
                    ClassSelectionList(onClassSelected = { viewModel.selectClass(it) })
                }
                PacksNavigationLevel.SUBJECTS -> {
                    val subjects = displayPacks.map { it.subject }.distinct()
                    SubjectSelectionList(
                        subjects = subjects,
                        onSubjectSelected = { viewModel.selectSubject(it) }
                    )
                }
                PacksNavigationLevel.CHAPTERS -> {
                    val chapters = displayPacks.filter { it.subject == selectedSubject }
                    ChapterList(
                        chapters = chapters,
                        downloadedPacks = downloadedPacks,
                        isDownloadingMap = isDownloadingMap,
                        onDownloadClick = { pack -> viewModel.downloadPack(pack.id, pack.subject, pack.title) },
                        onPlayClick = { onNavigateToQuiz(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun ClassSelectionList(onClassSelected: (Int) -> Unit) {
    val classes = listOf(9, 10, 11, 12)
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Select Your Class", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(classes) { level ->
            SelectionWideCard(
                title = "Class ${level}th",
                subtitle = "Secondary School Certificate",
                icon = Icons.Default.School,
                color = MaterialTheme.colorScheme.primary,
                onClick = { onClassSelected(level) }
            )
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun SubjectSelectionList(subjects: List<String>, onSubjectSelected: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Available Subjects", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        if (subjects.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("No subjects available for this class yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(subjects) { subject ->
                SelectionWideCard(
                    title = subject,
                    subtitle = "Explore chapters and quizzes",
                    icon = when(subject.lowercase()) {
                        "physics" -> Icons.Default.Science
                        "biology" -> Icons.Default.Biotech
                        "mathematics", "math" -> Icons.Default.Functions
                        "chemistry" -> Icons.Default.AutoGraph
                        else -> Icons.Default.Book
                    },
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = { onSubjectSelected(subject) }
                )
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun SelectionWideCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun ChapterList(
    chapters: List<com.zamnia.quizapp.data.model.Pack>,
    downloadedPacks: List<com.zamnia.quizapp.data.local.entities.DownloadedPackageEntity>,
    isDownloadingMap: Map<String, Boolean>,
    onDownloadClick: (com.zamnia.quizapp.data.model.Pack) -> Unit,
    onPlayClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }
        items(chapters) { pack ->
            val downloadedInfo = downloadedPacks.find { it.packageId == pack.id }
            val isDownloading = isDownloadingMap[pack.id] ?: false
            
            val icon = when(pack.iconName) {
                "Science" -> Icons.Default.Science
                "Biotech" -> Icons.Default.Biotech
                "Functions" -> Icons.Default.Functions
                else -> Icons.Default.Book
            }
            
            val packColor = try {
                Color(android.graphics.Color.parseColor(pack.colorHex))
            } catch (e: Exception) {
                MaterialTheme.colorScheme.primary
            }

            PackItem(
                title = pack.title,
                questions = pack.questionCount,
                size = "1.5 MB",
                icon = icon,
                color = packColor,
                status = when {
                    isDownloading -> PackStatus.Downloading(50)
                    downloadedInfo?.isDownloaded == true -> PackStatus.Offline
                    else -> PackStatus.Download
                },
                onDownloadClick = { onDownloadClick(pack) },
                onPlayClick = { onPlayClick(pack.id) }
            )
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun PackItem(
    title: String,
    questions: String,
    size: String,
    icon: ImageVector,
    color: Color,
    status: PackStatus,
    onDownloadClick: () -> Unit = {},
    onPlayClick: () -> Unit = {}
) {
    val isLocked = status is PackStatus.Locked
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(enabled = !isLocked) { if (status is PackStatus.Offline) onPlayClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (!isLocked) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(20.dp)
                            .background(color.copy(alpha = 0.2f))
                    )
                }
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = questions, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (size.isNotEmpty()) {
                        Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.outlineVariant, CircleShape))
                        Text(text = size, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            when (status) {
                PackStatus.Offline -> {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)),
                        onClick = onDownloadClick // Allow re-downloading/syncing
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sync", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                PackStatus.Download -> {
                    OutlinedButton(onClick = onDownloadClick, shape = CircleShape) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Download")
                    }
                }
                is PackStatus.Downloading -> {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
                PackStatus.Locked -> {}
            }
        }
    }
}

package com.zamnia.quizapp.ui.zamnia

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpCenter
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zamnia.quizapp.ui.wallet.TransferState
import com.zamnia.quizapp.ui.wallet.WalletViewModel
import com.zamnia.quizapp.ui.dashboard.DashboardViewModel
import com.zamnia.quizapp.ui.auth.AuthViewModel
import com.zamnia.quizapp.ui.zamnia.components.ZamniaBottomNavigation
import com.zamnia.quizapp.ui.zamnia.components.ZamniaTextField

@Composable
fun ZamniaWalletScreen(
    onBack: () -> Unit,
    onNavigateToHub: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPacks: () -> Unit,
    walletViewModel: WalletViewModel = viewModel(),
    dashboardViewModel: DashboardViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val userProfile by dashboardViewModel.userProfile.collectAsState()
    val transferState by walletViewModel.transferState.collectAsState()
    val recipientUser by walletViewModel.recipientUser.collectAsState()
    val remainingTransfers by walletViewModel.remainingTransfers.collectAsState()
    val isOnline by authViewModel.isOnline.collectAsState()

    // Automatically refresh wallet data when screen opens or internet comes back
    LaunchedEffect(isOnline) {
        if (isOnline) {
            walletViewModel.refreshWallet()
        }
    }

    ZamniaWalletContent(
        userProfile = userProfile,
        transferState = transferState,
        recipientUser = recipientUser,
        remainingTransfers = remainingTransfers,
        isOnline = isOnline,
        onBack = onBack,
        onNavigateToHub = onNavigateToHub,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToPacks = onNavigateToPacks,
        onFindRecipient = { walletViewModel.findRecipient(it) },
        onTransferCoins = { id, amt -> walletViewModel.transferCoins(id, amt) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZamniaWalletContent(
    userProfile: com.zamnia.quizapp.data.model.User?,
    transferState: TransferState,
    recipientUser: com.zamnia.quizapp.data.model.User?,
    remainingTransfers: Int,
    isOnline: Boolean,
    onBack: () -> Unit,
    onNavigateToHub: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPacks: () -> Unit,
    onFindRecipient: (String) -> Unit,
    onTransferCoins: (String, Long) -> Unit
) {
    var friendId by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    LaunchedEffect(friendId) {
        if (friendId.length == 6) {
            onFindRecipient(friendId)
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
                currentRoute = "wallet",
                onNavigateToHub = onNavigateToHub,
                onNavigateToWallet = {},
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToPacks = onNavigateToPacks
            ) 
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Balance Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Background Gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f))
                                )
                            )
                    )
                    
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Available Balance",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.5f),
                                shape = CircleShape,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val statusColor = if (isOnline) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                                    val statusText = if (isOnline) "Live" else "Offline"
                                    
                                    Box(modifier = Modifier.size(8.dp).background(statusColor, CircleShape))
                                    Text(text = statusText, style = MaterialTheme.typography.labelSmall, color = statusColor)
                                }
                            }
                        }
                        
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = if (isOnline) (userProfile?.coinBalance?.toString() ?: "0") else "---",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                        )
                        
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = Icons.Default.Cached,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp)
                            )
                            val used = 2 - remainingTransfers
                            Text(
                                text = "Today's Limit: $used/2 used ($remainingTransfers remaining)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
            
            // Input Fields
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ZamniaTextField(
                    value = friendId,
                    onValueChange = { if (it.length <= 6) friendId = it },
                    label = "Friend's 6-Digit ID",
                    placeholder = "e.g. 482910",
                    icon = Icons.Default.Fingerprint,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                
                ZamniaTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = "Coin Amount",
                    placeholder = "0",
                    icon = Icons.Default.Toll,
                    trailing = {
                        TextButton(onClick = { amount = (userProfile?.coinBalance ?: 0L).toString() }) {
                            Text("MAX", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            
            // Recipient Preview
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.4f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    
                    val isFound = recipientUser != null
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .border(
                                width = 1.dp,
                                color = if (isFound) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFound) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.HelpCenter,
                            contentDescription = null,
                            tint = if (isFound) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                    
                    if (isFound) {
                        if (recipientUser != null) {
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = recipientUser.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            
            if (transferState is TransferState.Error) {
                Text(
                    text = (transferState as TransferState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Transfer Button
            Button(
                onClick = { 
                    val coins = amount.toLongOrNull() ?: 0L
                    onTransferCoins(friendId, coins)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (transferState is TransferState.Success) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                ),
                shape = CircleShape,
                enabled = isOnline && 
                          remainingTransfers > 0 && 
                          transferState !is TransferState.Loading && 
                          recipientUser != null && 
                          amount.isNotEmpty()
            ) {
                when (transferState) {
                    is TransferState.Loading -> {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    }
                    is TransferState.Success -> {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Success", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    else -> {
                        Text(
                            text = "Authorize Transfer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp)) // Safe space at bottom
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F131D)
@Composable
fun WalletPreview() {
    com.zamnia.quizapp.ui.theme.ZamniaTheme {
        ZamniaWalletContent(
            userProfile = com.zamnia.quizapp.data.model.User("1", "482910", "hamza@zamnia.com", "Hamza Explorer", 1250L),
            transferState = TransferState.Idle,
            recipientUser = null,
            remainingTransfers = 2,
            isOnline = true,
            onBack = {},
            onNavigateToHub = {},
            onNavigateToSettings = {},
            onNavigateToPacks = {},
            onFindRecipient = {},
            onTransferCoins = { _, _ -> }
        )
    }
}

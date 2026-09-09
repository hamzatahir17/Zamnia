package com.zamnia.quizapp.ui.zamnia

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zamnia.quizapp.ui.quiz.QuizViewModel

enum class OptionState {
    IDLE, CORRECT, WRONG, DIMMED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZamniaQuizSessionScreen(
    packageId: String? = null,
    onBack: () -> Unit,
    onQuizFinished: (Int, Int, Int) -> Unit,
    viewModel: QuizViewModel = viewModel()
) {
    val questions by viewModel.questions.collectAsState()
    val currentIndex by viewModel.currentQuestionIndex.collectAsState()
    val selectedAnswer by viewModel.selectedAnswer.collectAsState()
    val isFinished by viewModel.isFinished.collectAsState()
    val hasNoQuestions by viewModel.hasNoQuestions.collectAsState()
    val score by viewModel.score.collectAsState()
    val coinsEarned by viewModel.coinsEarned.collectAsState()

    // Start quiz immediately when screen opens
    LaunchedEffect(Unit) {
        viewModel.startQuiz(0, packageId) 
    }

    LaunchedEffect(isFinished) {
        if (isFinished) {
            onQuizFinished(score, questions.size, coinsEarned)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Quiz Session", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (questions.isNotEmpty()) {
                // Progress Header
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "QUESTION ${currentIndex + 1} OF ${questions.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (currentIndex + 1).toFloat() / questions.size },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        val currentQuestion = questions[currentIndex]
                        // Question Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Box {
                                // Subtle glow
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .align(Alignment.TopEnd)
                                        .offset(x = 20.dp, y = (-20).dp)
                                        .blur(40.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
                                )
                                
                                Column(modifier = Modifier.padding(24.dp)) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        shape = CircleShape,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                            Text(currentQuestion.category.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = currentQuestion.question,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    itemsIndexed(questions[currentIndex].options) { index, option ->
                        val letter = ('A' + index).toString()
                        val currentQuestion = questions[currentIndex]
                        
                        val isCorrect = index == currentQuestion.correctAnswerIndex
                        val isSelected = selectedAnswer == index
                        
                        val state = when {
                            selectedAnswer == null -> OptionState.IDLE
                            isSelected && isCorrect -> OptionState.CORRECT
                            isSelected && !isCorrect -> OptionState.WRONG
                            isCorrect -> OptionState.CORRECT // Always show correct answer if someone else was picked
                            else -> OptionState.DIMMED
                        }

                        ZamniaOptionItem(
                            letter = letter,
                            text = option,
                            state = state,
                            onClick = { viewModel.submitAnswer(index) }
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (hasNoQuestions) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⚠️", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No questions found in this pack.", color = MaterialTheme.colorScheme.onSurface)
                            Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
                                Text("Go Back")
                            }
                        }
                    } else {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
fun ZamniaOptionItem(letter: String, text: String, state: OptionState, onClick: () -> Unit) {
    val successColor = Color(0xFF4CAF50)
    val errorColor = Color(0xFFE91E63)
    
    val (bgColor, borderColor, contentColor) = when (state) {
        OptionState.IDLE -> Triple(MaterialTheme.colorScheme.surfaceContainerLow, Color.White.copy(alpha = 0.05f), MaterialTheme.colorScheme.onSurface)
        OptionState.CORRECT -> Triple(successColor.copy(alpha = 0.15f), successColor.copy(alpha = 0.5f), successColor)
        OptionState.WRONG -> Triple(errorColor.copy(alpha = 0.15f), errorColor.copy(alpha = 0.5f), errorColor)
        OptionState.DIMMED -> Triple(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f), Color.White.copy(alpha = 0.02f), MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(width = 1.dp, color = borderColor),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(bgColor)
                    .border(1.dp, contentColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = letter,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor
            )
        }
    }
}

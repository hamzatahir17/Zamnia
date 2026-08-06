package com.zamnia.quizapp.ui.zamnia

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zamnia.quizapp.R
import kotlinx.coroutines.delay

@Composable
fun ZamniaSplashScreen(
    onSplashFinished: () -> Unit,
) {
    var progress by remember { mutableFloatStateOf(0f) }
    
    // Smooth progress animation
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing),
        label = "SplashProgress"
    )

    LaunchedEffect(Unit) {
        delay(300)
        progress = 0.4f
        delay(800)
        progress = 0.85f
        delay(1000)
        progress = 1.0f
        delay(800)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.splash_background)),
        contentAlignment = Alignment.Center
    ) {
        // Logo is now placed in a Box to ensure it stays at ABSOLUTE CENTER
        // matching the native splash screen's behavior perfectly.
        androidx.compose.foundation.Image(
            painter = painterResource(id = R.drawable.screen),
            contentDescription = "Zamnia Professional Logo",
            contentScale = ContentScale.FillBounds, // Forces exact width/height match
            modifier = Modifier
                .width(180.dp) // Adjusted width to match native scaling feel
                .height(180.dp)
        )

        // Loading section is placed at the bottom of the screen
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp) // Offset from the very bottom
        ) {
            // Custom Live Progress Bar
            Box(
                modifier = Modifier
                    .width(280.dp)
                    .height(6.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        CircleShape
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .background(MaterialTheme.colorScheme.secondary, CircleShape)
                )
                
                if ((animatedProgress > 0f) && (animatedProgress < 1f)) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxWidth(animatedProgress)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(10.dp)
                                .offset(x = 5.dp)
                                .blur(2.dp)
                                .background(Color.White, CircleShape)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SplashPreview() {
    com.zamnia.quizapp.ui.theme.ZamniaTheme {
        ZamniaSplashScreen(onSplashFinished = {})
    }
}

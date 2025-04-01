package com.example.a2dshootinggame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            _2DShootingGameTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GameWithSplash()
                }
            }
        }
    }
}

@Composable
fun GameWithSplash() {
    var showSplash by remember { mutableStateOf(true) }

    val alpha by animateFloatAsState(
        targetValue = if (showSplash) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        finishedListener = { if (!showSplash) showSplash = false }
    )

    LaunchedEffect(Unit) {
        delay(2000)
        showSplash = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!showSplash || alpha > 0) {
            ShootingGame()
        }

        if (showSplash) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(Color(0xFFFF6B6B), Color(0xFF4ECDC4), Color(0xFF45B7D1))
                        )
                    )
                    .alpha(alpha)
            ) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "2D Shooting",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Adventure",
                        fontSize = 32.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ShootingGame() {
    var playerPosition by remember { mutableStateOf(Offset(100f, 100f)) }
    val bullets = remember { mutableStateListOf<Bullet>() }
    val enemies = remember { mutableStateListOf<Enemy>() }
    var score by remember { mutableStateOf(0) }
    var gameOver by remember { mutableStateOf(false) }
    var gunAngle by remember { mutableStateOf(0f) }
    val playerColor = Color.hsl(Random.nextFloat() * 360f, 0.8f, 0.6f)

    val playerAnimX = animateFloatAsState(targetValue = playerPosition.x, animationSpec = tween(100))
    val playerAnimY = animateFloatAsState(targetValue = playerPosition.y, animationSpec = tween(100))
    val gunAnimAngle = animateFloatAsState(targetValue = gunAngle, animationSpec = tween(100))

    LaunchedEffect(gameOver) {
        if (!gameOver) {
            while (!gameOver) {
                delay(16L)
                if (Random.nextFloat() < 0.02f && enemies.size < 10) {
                    enemies.add(
                        Enemy(
                            position = Offset(Random.nextFloat() * 500f, -20f),
                            color = Color.hsl(Random.nextFloat() * 360f, 0.8f, 0.6f)
                        )
                    )
                }
                bullets.forEach { it.update() }
                bullets.removeAll { it.isOffScreen() }
                enemies.forEach { it.update() }

                val bulletsToRemove = mutableListOf<Bullet>()
                val enemiesToRemove = mutableListOf<Enemy>()
                bullets.forEach { bullet ->
                    enemies.forEach { enemy ->
                        if (enemy.isHit(bullet.position)) {
                            bulletsToRemove.add(bullet)
                            enemiesToRemove.add(enemy)
                            score += 10
                        }
                    }
                }
                bullets.removeAll(bulletsToRemove)
                enemies.removeAll(enemiesToRemove)
                enemies.removeAll { it.isOffScreen() }
                enemies.forEach { enemy ->
                    if (calculateDistance(enemy.position, Offset(playerAnimX.value, playerAnimY.value)) < 30f) {
                        gameOver = true
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    if (!gameOver) {
                        playerPosition = Offset(
                            (playerPosition.x + dragAmount.x).coerceIn(0f, size.width - 50f),
                            (playerPosition.y + dragAmount.y).coerceIn(0f, size.height - 50f)
                        )
                        gunAngle = atan2(
                            change.position.y - playerAnimY.value,
                            change.position.x - playerAnimX.value
                        ) * 180f / Math.PI.toFloat()

                        if (change.position.y < size.height - 100) {
                            bullets.add(
                                Bullet(
                                    position = Offset(
                                        playerAnimX.value + 20f * cos(gunAngle * Math.PI / 180f).toFloat(),
                                        playerAnimY.value + 20f * sin(gunAngle * Math.PI / 180f).toFloat()
                                    ),
                                    angle = gunAngle * Math.PI / 180f,
                                    color = Color.hsl(Random.nextFloat() * 360f, 0.8f, 0.6f)
                                )
                            )
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw player
            drawCircle(
                color = playerColor,
                radius = 20f,
                center = Offset(playerAnimX.value, playerAnimY.value)
            )

            // Draw gun
            rotate(degrees = gunAnimAngle.value, pivot = Offset(playerAnimX.value, playerAnimY.value)) {
                drawRect(
                    color = Color.Gray,
                    topLeft = Offset(playerAnimX.value, playerAnimY.value - 5f),
                    size = androidx.compose.ui.geometry.Size(30f, 10f)
                )
            }

            // Draw bullets
            bullets.forEach { bullet ->
                drawCircle(
                    color = bullet.color,
                    radius = 5f,
                    center = bullet.position
                )
            }

            // Draw enemies
            enemies.forEach { enemy ->
                drawCircle(
                    color = enemy.color,
                    radius = 15f,
                    center = enemy.position
                )
            }
        }

        Text(
            text = "Score: $score",
            color = Color.White,
            fontSize = 20.sp,
            modifier = Modifier.padding(16.dp)
        )

        if (gameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
            ) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Game Over\nScore: $score",
                        color = Color.White,
                        fontSize = 32.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = {
                            playerPosition = Offset(100f, 100f)
                            bullets.clear()
                            enemies.clear()
                            score = 0
                            gameOver = false
                            gunAngle = 0f
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B6B))
                    ) {
                        Text("Retry", color = Color.White, fontSize = 20.sp)
                    }
                }
            }
        }
    }
}

data class Bullet(
    var position: Offset,
    val angle: Double,
    val speed: Float = 10f,
    val color: Color
) {
    fun update() {
        position = Offset(
            (position.x + cos(angle) * speed).toFloat(),
            (position.y + sin(angle) * speed).toFloat()
        )
    }

    fun isOffScreen(): Boolean = position.x < 0 || position.x > 500 || position.y < 0 || position.y > 1000
}

data class Enemy(
    var position: Offset,
    val speed: Float = 2f,
    val color: Color
) {
    fun update() {
        position = Offset(position.x, position.y + speed)
    }

    fun isOffScreen(): Boolean = position.y > 1000

    fun isHit(bulletPos: Offset): Boolean = calculateDistance(position, bulletPos) < 20f
}

fun calculateDistance(p1: Offset, p2: Offset): Float {
    return kotlin.math.sqrt((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y))
}

@Composable
fun _2DShootingGameTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFBB86FC),
            secondary = Color(0xFF03DAC5),
            background = Color.Black
        ),
        content = content
    )
}
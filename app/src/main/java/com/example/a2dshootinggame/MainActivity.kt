package com.example.bubbleshooter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BubbleShooterGame()
        }
    }
}

@Composable
fun BubbleShooterGame() {
    var gunPosition by remember { mutableStateOf(300f) }
    val bullets = remember { mutableStateListOf<Bullet>() }
    val enemies = remember { mutableStateListOf<Enemy>() }
    var score by remember { mutableStateOf(0) }
    var gameOver by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (!gameOver) {
            delay(16L)
            if (Random.nextFloat() < 0.03f) {
                enemies.add(Enemy(Offset(Random.nextFloat() * 600f, 0f)))
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

            enemies.forEach { enemy ->
                if (enemy.position.y >= 900f && abs(enemy.position.x - gunPosition) < 50f) {
                    gameOver = true
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures {
                    bullets.add(Bullet(Offset(gunPosition, 880f)))
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onPress = { bullets.add(Bullet(Offset(gunPosition, 880f))) })
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                color = Color.Gray,
                topLeft = Offset(gunPosition - 25f, 900f),
                size = androidx.compose.ui.geometry.Size(50f, 20f)
            )
        }

        bullets.forEach { bullet ->
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(color = bullet.color, radius = 10f, center = bullet.position)
            }
        }

        enemies.forEach { enemy ->
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(color = enemy.color, radius = 50f, center = enemy.position)
            }
        }

        Text("Score: $score", color = Color.White, fontSize = 20.sp, modifier = Modifier.padding(16.dp))

        if (gameOver) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f))) {
                Column(modifier = Modifier.align(Alignment.Center)) {
                    Text("Game Over", color = Color.White, fontSize = 32.sp)
                    Text("Score: $score", color = Color.White, fontSize = 24.sp)
                    Button(onClick = {
                        gunPosition = 300f
                        bullets.clear()
                        enemies.clear()
                        score = 0
                        gameOver = false
                    }) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

data class Bullet(var position: Offset, val speed: Float = 12f, val color: Color = Color.Yellow) {
    fun update() {
        position = Offset(position.x, position.y - speed)
    }
    fun isOffScreen() = position.y < 0
}

data class Enemy(var position: Offset, val speed: Float = 4f, val color: Color = Color.Red) {
    fun update() {
        position = Offset(position.x, position.y + speed)
    }
    fun isHit(bulletPos: Offset) = (position - bulletPos).getDistance() < 60f
}

fun Offset.getDistance(): Float = sqrt(x * x + y * y)

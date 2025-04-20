package com.example.bubbleshooter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
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
    val explosions = remember { mutableStateListOf<Explosion>() }

    var score by remember { mutableStateOf(0) }
    var level by remember { mutableStateOf(1) }
    var gameOver by remember { mutableStateOf(false) }

    LaunchedEffect(gameOver) {
        while (!gameOver) {
            delay(16L) // ~60 FPS

            // Spawn new enemies based on level
            if (Random.nextFloat() < (0.01f + 0.002f * level)) {
                enemies.add(Enemy.createRandom(level))
            }

            // Update all bullets, enemies, explosions
            bullets.forEach { it.update() }
            enemies.forEach { it.update() }
            explosions.forEach { it.update() }

            // Remove off-screen or finished objects
            bullets.removeAll { it.isOffScreen() }
            enemies.removeAll { it.isOffScreen() }
            explosions.removeAll { it.isFinished() }

            // Bullet hits
            val hitBullets = mutableListOf<Bullet>()
            val hitEnemies = mutableListOf<Enemy>()
            for (bullet in bullets) {
                for (enemy in enemies) {
                    if (enemy.isHit(bullet.position)) {
                        hitBullets.add(bullet)
                        enemy.health -= 1
                        explosions.add(Explosion(enemy.position))
                        if (enemy.health <= 0) {
                            hitEnemies.add(enemy)
                            score += enemy.points
                        }
                        break
                    }
                }
            }
            bullets.removeAll(hitBullets)
            enemies.removeAll(hitEnemies)

            // Increase level every 100 points
            level = (score / 100) + 1

            // Check game over (enemy collides gun)
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
                    if (!gameOver) {
                        bullets.add(Bullet(position = Offset(gunPosition, 880f)))
                    }
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    gunPosition = change.position.x.coerceIn(25f, 575f)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw gun
            drawGun(gunPosition)

            // Draw bullets
            bullets.forEach { bullet ->
                drawCircle(color = bullet.color, radius = 10f, center = bullet.position)
            }

            // Draw enemies
            enemies.forEach { enemy ->
                drawCircle(color = enemy.color, radius = enemy.radius, center = enemy.position)
            }

            // Draw explosions
            explosions.forEach { explosion ->
                drawCircle(color = explosion.color, radius = explosion.radius, center = explosion.position)
            }
        }

        // UI elements
        Column(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart)
        ) {
            Text("Score: $score", color = Color.White, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Level: $level", color = Color.White, fontSize = 16.sp)
        }

        if (gameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Game Over", color = Color.White, fontSize = 36.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Final Score: $score", color = Color.White, fontSize = 24.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Reached Level: $level", color = Color.White, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            gunPosition = 300f
                            bullets.clear()
                            enemies.clear()
                            explosions.clear()
                            score = 0
                            level = 1
                            gameOver = false
                        }
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

// Drawing extensions
fun DrawScope.drawGun(position: Float) {
    drawRect(
        color = Color.Gray,
        topLeft = Offset(position - 25f, 900f),
        size = androidx.compose.ui.geometry.Size(50f, 20f)
    )
}

// Bullet class
data class Bullet(
    var position: Offset,
    private val speed: Float = 14f,
    val color: Color = Color.Yellow
) {
    fun update() {
        position = position.copy(y = position.y - speed)
    }
    fun isOffScreen() = position.y < 0
}

// Enemy class
data class Enemy(
    var position: Offset,
    var health: Int,
    val speed: Float,
    val radius: Float,
    val color: Color,
    val points: Int
) {
    fun update() {
        position = position.copy(y = position.y + speed)
    }
    fun isOffScreen() = position.y > 1000f
    fun isHit(bulletPos: Offset): Boolean {
        return (position - bulletPos).getDistance() < radius + 10f
    }

    companion object {
        fun createRandom(level: Int): Enemy {
            return when (Random.nextInt(3)) {
                0 -> Enemy(
                    position = Offset(Random.nextFloat() * 600f, 0f),
                    health = 1,
                    speed = 4f + level * 0.5f,
                    radius = 40f,
                    color = Color.Red,
                    points = 10
                )
                1 -> Enemy(
                    position = Offset(Random.nextFloat() * 600f, 0f),
                    health = 2,
                    speed = 3f + level * 0.4f,
                    radius = 45f,
                    color = Color.Magenta,
                    points = 20
                )
                else -> Enemy(
                    position = Offset(Random.nextFloat() * 600f, 0f),
                    health = 3,
                    speed = 2f + level * 0.3f,
                    radius = 50f,
                    color = Color.Cyan,
                    points = 30
                )
            }
        }
    }
}

// Explosion effect
data class Explosion(
    var position: Offset,
    var radius: Float = 10f,
    val color: Color = Color.Yellow,
    var life: Int = 10
) {
    fun update() {
        radius += 4f
        life -= 1
    }
    fun isFinished() = life <= 0
}

// Helper
fun Offset.getDistance(): Float = sqrt(x * x + y * y)

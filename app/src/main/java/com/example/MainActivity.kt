package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Brutalist Obsidian Dark Neon Color Scheme
private val CyberBackground = Color(0xFF0C0C0F)
private val CyberSurface = Color(0xFF141419)
private val CyberSecondarySurface = Color(0xFF1B1B22)
private val CyberPrimary = Color(0xFFFF5722) // Cyber Neon Orange Force
private val CyberGreenAccent = Color(0xFF00E676) // Toxic Toxic Green Indicators
private val CyberBlueAccent = Color(0xFF00E5FF) // Cyber Tech Blue
private val CyberCardBorder = Color(0xFF262630)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = CyberPrimary,
                    background = CyberBackground,
                    surface = CyberSurface,
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    DashboardScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CyberBackground)
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    
    // Core states observed directly from the CoreController Foreground Service
    val isRunning by CoreController.isServiceRunning.collectAsState()
    val fps by CoreController.currentFps.collectAsState()
    val latency by CoreController.currentLatency.collectAsState()
    val logs by CoreController.logsList.collectAsState()
    val currentHp by CoreController.currentHp.collectAsState()
    val steering by CoreController.activeSteering.collectAsState()
    val detected by CoreController.detectedTargets.collectAsState()
    
    // UI control bindings
    var showExplanation by remember { mutableStateOf(false) }
    var antiRecoilX by remember { mutableStateOf(CoreController.antiRecoilStrengthX) }
    var antiRecoilY by remember { mutableStateOf(CoreController.antiRecoilStrengthY) }
    var healThresh by remember { mutableStateOf(CoreController.healingThreshold) }
    var jitterFactor by remember { mutableStateOf(CoreController.antiCheatJitterLevel) }
    var selectedPackage by remember { mutableStateOf(CoreController.selectedGamePackage) }

    // Service Status Verification
    var hasAccessibility by remember { mutableStateOf(InputDispatcher.isServiceEnabled()) }
    var hasOverlayPermission by remember { mutableStateOf(true) }

    // On-resume updater callback
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasAccessibility = InputDispatcher.isServiceEnabled()
                hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Settings.canDrawOverlays(context)
                } else {
                    true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LazyColumn(
        modifier = modifier.testTag("agaa_settings_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header Name Plate
        item {
            HeaderSection(
                isRunning = isRunning,
                onExplainToggle = { showExplanation = !showExplanation }
            )
        }

        // Modular Workflow Explanation Diagram
        item {
            AnimatedVisibility(visible = showExplanation) {
                WorkflowDiagramSection()
            }
        }

        // Checklist of Android System Requirements & Actions
        item {
            SystemStatusCard(
                hasAccessibility = hasAccessibility,
                hasOverlay = hasOverlayPermission,
                onRequestAccessibility = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    context.startActivity(intent)
                    Toast.makeText(context, "Locate 'AI Game Automation' in settings and enable.", Toast.LENGTH_LONG).show()
                },
                onRequestOverlay = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                }
            )
        }

        // Action controls to Boot, Trigger or Shutdown the AI core agent
        item {
            PrimaryActivationCard(
                isRunning = isRunning,
                hasOverlayPermission = hasOverlayPermission,
                onToggleService = {
                    if (isRunning) {
                        val intent = Intent(context, CoreController::class.java).apply {
                            action = "STOP_SERVICE"
                        }
                        context.startService(intent)
                        CoreController.addLog("SYSTEM USER SHUTDOWN: Agent core stopped manually.")
                    } else {
                        if (!hasOverlayPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            Toast.makeText(context, "Grant floating overlay permission first!", Toast.LENGTH_SHORT).show()
                        } else {
                            val intent = Intent(context, CoreController::class.java)
                            context.startService(intent)
                            CoreController.addLog("SYSTEM DEPLOYED: Listening overlay channels.")
                        }
                    }
                }
            )
        }

        // Live HUD stats and simulated TFLite bounding outputs
        item {
            TelemetryHUDCard(
                isRunning = isRunning,
                fps = fps,
                latency = latency,
                hp = currentHp,
                steering = steering,
                detected = detected
            )
        }

        // PackageManager Game Launch Controller
        item {
            GameLauncherCard(
                context = context,
                selectedPackage = selectedPackage,
                onPackageSelected = { pkg ->
                    selectedPackage = pkg
                    CoreController.selectedGamePackage = pkg
                }
            )
        }

        // Anti-Cheat (Tactile Hand Jittering) Calibration Slider Card
        item {
            AntiCheatSecurityCard(
                jitterFactor = jitterFactor,
                onJitterChange = {
                    jitterFactor = it
                    CoreController.antiCheatJitterLevel = it
                }
            )
        }

        // Anti-Recoil Vector Controls Card
        item {
            AntiRecoilControlCard(
                strengthX = antiRecoilX,
                strengthY = antiRecoilY,
                onStrengthXChange = { 
                    antiRecoilX = it
                    CoreController.antiRecoilStrengthX = it
                },
                onStrengthYChange = { 
                    antiRecoilY = it
                    CoreController.antiRecoilStrengthY = it
                }
            )
        }

        // Adaptive HUD Healing Monitor Card
        item {
            AdaptiveHealingCard(
                healThreshold = healThresh,
                currentHp = currentHp,
                onThresholdChange = { 
                    healThresh = it
                    CoreController.healingThreshold = it
                }
            )
        }

        // Real-Time System Logs & Terminal output viewer
        item {
            LiveLogsTerminalCard(
                logs = logs,
                onClear = { CoreController.logsList.value = listOf("Log reset by user.") },
                onInjectDetections = {
                    // Educational mock injection: triggers automated firing routine
                    // demonstrating coordinate calculation pipeline flow to student/reviewer
                    CoreController.addLog("[EDUCATIONAL INJECTION]: Triggering simulated Opponent Detection overlay...")
                    CoroutineScope(Dispatchers.Default).launch {
                        delay(200)
                        if (InputDispatcher.isServiceEnabled()) {
                            InputDispatcher.getInstance()?.performClick(960f, 540f, jitterFactor)
                            CoreController.addLog("[Tactile Trigger]: Click emulated at center 960, 540.")
                        } else {
                            CoreController.addLog("[TACTILE FAIL]: Enable Accessibility to visualize Touch Dispatcher.")
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun HeaderSection(isRunning: Boolean, onExplainToggle: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CyberCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AI GAME AUTOMATION",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Real-Time OpenCV -> Brain -> Tactile Accessibility Hub",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }

                IconButton(
                    onClick = onExplainToggle,
                    modifier = Modifier.background(CyberSecondarySurface, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Show Architecture Info",
                        tint = CyberPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = CyberCardBorder)
            Spacer(modifier = Modifier.height(12.dp))

            // Pulse Status Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 0.8f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse"
                )

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .scale(if (isRunning) scale else 1.0f)
                        .background(if (isRunning) CyberGreenAccent else Color.Red, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRunning) "AGENT ACTIVE - INFERENCE RUNNING" else "AGENT OFF - DISCONNECTED",
                    color = if (isRunning) CyberGreenAccent else Color.Red,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun WorkflowDiagramSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberSecondarySurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CyberPrimary.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "SYSTEM ARCHITECTURE WORKFLOW (THE EYE -> THE BRAIN -> THE HAND)",
                color = CyberPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            val steps = listOf(
                "THE EYE" to "Screen -> ImageReader Buffer -> INT8 YOLOv8 Quantized CNN -> Floating mapping",
                "THE BRAIN" to "CoreController Service -> Collision Checking -> Pull index multipliers -> Steering safe directions",
                "THE HAND" to "InputDispatcher -> Accessibility Gesture APIs -> Sinusoidal Jitter Splines -> Device Screen Click"
            )

            steps.forEachIndexed { index, pair ->
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = "[${index + 1}] ${pair.first}: ",
                        color = CyberBlueAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = pair.second,
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
                if (index < steps.lastIndex) {
                    Text(
                        text = "       ↓",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SystemStatusCard(
    hasAccessibility: Boolean,
    hasOverlay: Boolean,
    onRequestAccessibility: () -> Unit,
    onRequestOverlay: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CyberCardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "ANDROID SYSTEM CONTROLS REQUIRED",
                color = Color.Gray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Accessibility Check Box
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Accessibility Touch Controller",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (hasAccessibility) "Granted" else "Requires manual config to simulate tactile swipes.",
                        color = if (hasAccessibility) CyberGreenAccent else Color.LightGray,
                        fontSize = 11.sp
                    )
                }

                if (!hasAccessibility) {
                    Button(
                        onClick = onRequestAccessibility,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("enable_accessibility_button")
                    ) {
                        Text("Config", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Granted",
                        tint = CyberGreenAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = CyberCardBorder)
            Spacer(modifier = Modifier.height(12.dp))

            // Overlay Check Box
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Overlay HUD Floating Window",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (hasOverlay) "Granted" else "Allows drawing quick action meters over active games.",
                        color = if (hasOverlay) CyberGreenAccent else Color.LightGray,
                        fontSize = 11.sp
                    )
                }

                if (!hasOverlay) {
                    Button(
                        onClick = onRequestOverlay,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("enable_overlay_button")
                    ) {
                        Text("Grant", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Granted",
                        tint = CyberGreenAccent
                    )
                }
            }
        }
    }
}

@Composable
fun PrimaryActivationCard(
    isRunning: Boolean,
    hasOverlayPermission: Boolean,
    onToggleService: () -> Unit
) {
    Button(
        onClick = onToggleService,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isRunning) Color.Red else CyberPrimary
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("primary_activation_button"),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Icon(
            imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
            contentDescription = if (isRunning) "Stop service" else "Start service",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isRunning) "SHUTDOWN AI AUTOMATION AGENT" else "DEPLOY AI AUTOMATION AGENT (FOREGROUND)",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun TelemetryHUDCard(
    isRunning: Boolean,
    fps: Int,
    latency: Long,
    hp: Float,
    steering: SteeringVector?,
    detected: List<DetectedObject>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CyberCardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "REAL-TIME TELEMETRY STATS HUD",
                color = Color.Gray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                // FPS Telemetry
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("Inference Speed", color = Color.Gray, fontSize = 9.sp)
                    Text(
                        text = if (isRunning) "${latency}ms" else "--",
                        color = CyberBlueAccent,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text("INT8 GPU Core", color = Color.Gray, fontSize = 8.sp)
                }

                // AI Engine State
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("Tactical FPS", color = Color.Gray, fontSize = 9.sp)
                    Text(
                        text = if (isRunning) "$fps" else "--",
                        color = CyberGreenAccent,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text("33ms loop cycle", color = Color.Gray, fontSize = 8.sp)
                }

                // Health Analyzer Indicator
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("Parsed HUD HP", color = Color.Gray, fontSize = 9.sp)
                    Text(
                        text = if (isRunning) "${String.format("%.0f", hp * 100)}%" else "--",
                        color = when {
                            hp > 0.5f -> CyberGreenAccent
                            hp > 0.3f -> Color.Yellow
                            else -> Color.Red
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text("RGB HUD Scan", color = Color.Gray, fontSize = 8.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = CyberCardBorder)
            Spacer(modifier = Modifier.height(14.dp))

            // AI Target Detections
            Text(
                text = "ACTIVE YOLO BOUNDS DETECTED ON CORE FRAME",
                color = CyberPrimary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (!isRunning) {
                Text(
                    text = "Activate the service engine to launch CNN frames overlay scanner.",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
            } else {
                if (detected.isEmpty()) {
                    Text(
                        text = "Searching screen matrix loops. No high-confidence opponent found...",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                } else {
                    detected.forEach { obj ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(CyberSecondarySurface, RoundedCornerShape(6.dp))
                                .border(1.dp, CyberCardBorder, RoundedCornerShape(6.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            if (obj.category == "enemy") Color.Red else CyberGreenAccent,
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = obj.label,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Conf: ${String.format("%.0f", obj.confidence * 100)}% | [X:${obj.boundingBox.centerX().toInt()} Y:${obj.boundingBox.centerY().toInt()}]",
                                color = Color.LightGray,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Minimap steering instructions
                steering?.let { st ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CyberBlueAccent.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .border(1.dp, CyberBlueAccent.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsRun,
                            contentDescription = "Safe Zone directions",
                            tint = CyberBlueAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "PATHFINDING VECTOR TO SAFE ZONE",
                                color = CyberBlueAccent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${st.recommendation} (Steer: ${String.format("%.0f", st.directionAngle)}° at ${String.format("%.1f", st.distancePx)}m)",
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GameLauncherCard(
    context: Context,
    selectedPackage: String,
    onPackageSelected: (String) -> Unit
) {
    var installedGames by remember { mutableStateOf<List<GameTarget>>(emptyList()) }
    
    // Scan for package mappings on initialize
    LaunchedEffect(Unit) {
        installedGames = LaunchModule.getInstalledGames(context)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CyberCardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "GAME LAUNCH TARGET SCANNER",
                color = Color.Gray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(12.dp))

            installedGames.forEach { game ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(
                            if (selectedPackage == game.packageName) CyberSecondarySurface else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onPackageSelected(game.packageName) }
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = (selectedPackage == game.packageName),
                            onClick = { onPackageSelected(game.packageName) },
                            colors = RadioButtonDefaults.colors(selectedColor = CyberPrimary)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = game.name,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = game.packageName,
                                color = Color.LightGray,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    if (game.isInstalled) {
                        Button(
                            onClick = {
                                LaunchModule.launchGame(context, game.packageName) {
                                    // Start service immediately when game target runs
                                    val intent = Intent(context, CoreController::class.java)
                                    context.startService(intent)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberGreenAccent),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Launch", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            text = "Uninstalled",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AntiCheatSecurityCard(
    jitterFactor: Float,
    onJitterChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CyberCardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "ANTI-CHEAT SANITIZER (DYNAMIC HAND JITTERING)",
                color = Color.Gray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Neutralizes static touches, linear drags and strict frame latency targets to evade modern engine scanners.",
                color = Color.LightGray,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "Gaussian Jitter Radius:",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${String.format("%.1f", jitterFactor)} px",
                    color = CyberPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            
            Slider(
                value = jitterFactor,
                onValueChange = onJitterChange,
                valueRange = 0.0f..15.0f,
                colors = SliderDefaults.colors(
                    activeTrackColor = CyberPrimary,
                    thumbColor = CyberPrimary
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Low (Predictable)", color = Color.Gray, fontSize = 9.sp)
                Text("Standard Jitter", color = CyberGreenAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text("High (Heavy Jitter)", color = Color.Gray, fontSize = 9.sp)
            }
        }
    }
}

@Composable
fun AntiRecoilControlCard(
    strengthX: Float,
    strengthY: Float,
    onStrengthXChange: (Float) -> Unit,
    onStrengthYChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CyberCardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "ANTI-RECOIL TACTILE COMPENSATOR",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Tactile Compensator",
                    tint = CyberPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Vert pull (Y axis)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Vertical Grip Strength (Y Pull):", color = Color.White, fontSize = 12.sp)
                Text(
                    text = "${String.format("%.1f", strengthY)}x",
                    color = CyberPrimary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Slider(
                value = strengthY,
                onValueChange = onStrengthYChange,
                valueRange = 0.0f..10.0f,
                colors = SliderDefaults.colors(activeTrackColor = CyberPrimary, thumbColor = CyberPrimary)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Horiz correction (X axis)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Horizontal Deviation Dampening (X Pull):", color = Color.White, fontSize = 12.sp)
                Text(
                    text = "${String.format("%.1f", strengthX)}x",
                    color = CyberBlueAccent,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Slider(
                value = strengthX,
                onValueChange = onStrengthXChange,
                valueRange = 0.0f..5.0f,
                colors = SliderDefaults.colors(activeTrackColor = CyberBlueAccent, thumbColor = CyberBlueAccent)
            )
        }
    }
}

@Composable
fun AdaptiveHealingCard(
    healThreshold: Float,
    currentHp: Float,
    onThresholdChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CyberCardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "ADAPTIVE HEALTH MONITOR & AUTO-HEAL",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Icon(
                    imageVector = Icons.Default.HealthAndSafety,
                    contentDescription = "Auto Heal",
                    tint = CyberGreenAccent,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Threshold to trigger consume:", color = Color.White, fontSize = 12.sp)
                Text(
                    text = "${String.format("%.0f", healThreshold * 100)}% HP",
                    color = CyberGreenAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Slider(
                value = healThreshold,
                onValueChange = onThresholdChange,
                valueRange = 0.3f..0.8f,
                colors = SliderDefaults.colors(activeTrackColor = CyberGreenAccent, thumbColor = CyberGreenAccent)
            )

            // Bar visualizer representing live HUD parser
            Spacer(modifier = Modifier.height(8.dp))
            Text("Simulated HUD HP Status Bar:", color = Color.Gray, fontSize = 9.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            ) {
                // Background shadow track
                drawRect(color = Color(0xFF1E1E24))
                
                // Live active health filling
                val hpColor = when {
                    currentHp > 0.5f -> CyberGreenAccent
                    currentHp > 0.3f -> Color.Yellow
                    else -> Color.Red
                }
                drawRect(
                    color = hpColor,
                    size = size.copy(width = size.width * currentHp)
                )

                // Draggable indicator mark of our thresh limit
                val thresholdX = size.width * healThreshold
                drawLine(
                    color = Color.White,
                    start = Offset(thresholdX, 0f),
                    end = Offset(thresholdX, size.height),
                    strokeWidth = 3f
                )
            }
        }
    }
}

@Composable
fun LiveLogsTerminalCard(
    logs: List<String>,
    onClear: () -> Unit,
    onInjectDetections: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CyberCardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TACTICAL TELEMETRY LIVE TERMINAL",
                    color = Color.LightGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                
                Row {
                    Text(
                        text = "TEST INJECT",
                        color = CyberBlueAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .clickable { onInjectDetections() }
                            .padding(horizontal = 8.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "CLEAR",
                        color = Color.Red,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .clickable { onClear() }
                            .padding(horizontal = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            // Emulated CRT Shell terminal screen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color.Black, RoundedCornerShape(8.dp))
                    .border(1.dp, CyberSecondarySurface, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    reverseLayout = true // Scroll lists starting downwards
                ) {
                    itemsIndexed(logs.reversed()) { _, log ->
                        Text(
                            text = "> $log",
                            color = when {
                                log.contains("ALERT") || log.contains("CRITICAL") -> Color.Red
                                log.contains("ACTIVE") || log.contains("DEPLOYED") -> CyberGreenAccent
                                log.contains("INJECTION") -> CyberBlueAccent
                                else -> Color.LightGray
                            },
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

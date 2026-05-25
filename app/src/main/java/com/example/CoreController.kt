package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

class CoreController : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "CoreController"
        private const val CHANNEL_ID = "agaa_core_channel"
        private const val NOTIFICATION_ID = 99221

        // Companion states to stream directly to Jetpack Compose Dashboard UI
        val isServiceRunning = MutableStateFlow(false)
        val currentFps = MutableStateFlow(0)
        val currentLatency = MutableStateFlow(0L)
        val logsList = MutableStateFlow<List<String>>(listOf("System initialized."))
        val detectedTargets = MutableStateFlow<List<DetectedObject>>(emptyList())
        val currentHp = MutableStateFlow(1.0f)
        val activeSteering = MutableStateFlow<SteeringVector?>(null)

        // Config variables governed by Compose Settings
        var antiRecoilStrengthX = 1.0f
        var antiRecoilStrengthY = 4.5f
        var healingThreshold = 0.5f // 50% HP triggers healing consumable
        val lootFilters = mutableSetOf("loot_weapon", "loot_heal")
        var antiCheatJitterLevel = 6.0f // pixel jitter range
        var selectedGamePackage = "com.tencent.ig"

        fun addLog(msg: String) {
            val list = logsList.value.toMutableList()
            if (list.size > 80) list.removeAt(0)
            list.add(msg)
            logsList.value = list
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isLoopActive = false
    private var metricsJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "CoreController service created.")
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "STOP_SERVICE") {
            stopEngine()
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification("Ready for Game Capture..."))
        
        if (!isLoopActive) {
            startEngine()
        }
        return START_STICKY
    }

    private fun startEngine() {
        Log.d(TAG, "Starting Automation Core Threads...")
        isLoopActive = true
        isServiceRunning.value = true
        addLog("SYSTEM ACTIVATED: Holding CPU locks...")

        // 1. Acquire CPU WakeLock
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AGAA:CpuLock").apply {
            acquire(10 * 60 * 1000L /*10 minutes max lock*/)
        }

        // 2. Initialize quantized YOLOv8 models inside computer vision core
        VisionModule.initializeModel(this, useGpu = true, quantized = true)

        // 3. Spawns visual and tactical loops
        metricsJob = serviceScope.launch {
            var frameCount = 0
            var lastTime = System.currentTimeMillis()
            
            while (isLoopActive) {
                val tickStart = System.currentTimeMillis()

                // Simulating display buffer sizes for frame coordinates (1080p Standard)
                val width = 1920
                val height = 1080

                // Simulated bitmap read inside standard ImageReader screen loop
                val detections = VisionModule.processFrame(
                    Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888),
                    width,
                    height,
                    lootFilters
                )
                
                detectedTargets.value = detections
                currentLatency.value = VisionModule.getInferenceTime()

                // Auto Health Analysis & Adaptive Healing (Requirement 4)
                val hp = VisionModule.analyzeHealthBar(null, healingThreshold)
                currentHp.value = hp
                if (hp < healingThreshold) {
                    addLog("CRITICAL HP TRIGGERED: ${String.format("%.1f", hp * 100)}% HP. Emulating Healing Consumable swipe.")
                    performAutoHeal()
                    delay(3000) // Delay to complete simulated heal animation
                }

                // Process Tactical Decision Trees (The Brain -> Decisions)
                detections.forEach { obj ->
                    when (obj.category) {
                        "enemy" -> {
                            addLog("CV ALERT: Found Enemy Opponent (${String.format("%.1f", obj.confidence * 100)}%) at X:${obj.boundingBox.centerX().toInt()}, Y:${obj.boundingBox.centerY().toInt()}")
                            executeTacticalTouch(obj, width, height)
                        }
                        "loot_weapon", "loot_ammo", "loot_heal" -> {
                            addLog("CV DETECTED LOOT: Priority item - ${obj.label}. Collecting item...")
                            triggerLootClick(obj)
                        }
                    }
                }

                // Minimap update (Requirement 4 Pathfinding)
                val steering = VisionModule.calculateSteeringToSafeZone(width, height)
                activeSteering.value = steering

                // Calculate Live Execution Framerate
                frameCount++
                val now = System.currentTimeMillis()
                if (now - lastTime >= 1000) {
                    currentFps.value = frameCount
                    frameCount = 0
                    lastTime = now
                    updateNotification("Running frame analysis... FPS: ${currentFps.value} | LAT: ${currentLatency.value}ms")
                }

                // Tick sleep to maintain stable target cycles (~30FPS)
                val tickElapsed = System.currentTimeMillis() - tickStart
                val sleepTime = 33L - tickElapsed
                if (sleepTime > 0) {
                    delay(sleepTime)
                }
            }
        }

        // 4. Inject Dynamic float widget (Requirement 4 Widget control Toggle)
        Handler(Looper.getMainLooper()).post {
            setupFloatingControlWidget()
        }
    }

    private fun triggerLootClick(obj: DetectedObject) {
        val dispatcher = InputDispatcher.getInstance()
        if (dispatcher != null) {
            // Simulated loot button coordinate in target PUBG hud listing (usually right-mid screen)
            val lootButtonX = obj.boundingBox.centerX()
            val lootButtonY = obj.boundingBox.centerY()
            dispatcher.performClick(lootButtonX, lootButtonY, antiCheatJitterLevel)
        } else {
            addLog("WARNING: Loot collection skipped. Accessibility service NOT enabled!")
        }
    }

    private fun performAutoHeal() {
        val dispatcher = InputDispatcher.getInstance()
        if (dispatcher != null) {
            // Clicks HP pouch (standard layout is coordinate 540x950 on common HUD sizes)
            dispatcher.performClick(540f, 950f, antiCheatJitterLevel)
        } else {
            addLog("WARNING: Direct healing input failed. Target Accessibility service offline.")
        }
    }

    private fun executeTacticalTouch(target: DetectedObject, screenW: Int, screenH: Int) {
        val dispatcher = InputDispatcher.getInstance()
        if (dispatcher == null) {
            addLog("TACTICAL RECOIL WARN: Input Dispatcher inaccessible. Enable Accessibility.")
            return
        }

        // Tap fire trigger coordinates
        val fireButtonX = target.boundingBox.centerX()
        val fireButtonY = target.boundingBox.centerY()
        
        // Emulates jittered trigger click
        dispatcher.performClick(fireButtonX, fireButtonY, antiCheatJitterLevel)

        // Apply anti-recoil pull vectors (Requirement 4 Recoil compensation)
        if (antiRecoilStrengthY > 0f || antiRecoilStrengthX > 0f) {
            addLog("ANTI-RECOIL ACTIVE: Compensating recoil pull index Vector Y:-$antiRecoilStrengthY")
            dispatcher.performSwipeForRecoil(
                startX = fireButtonX,
                startY = fireButtonY,
                offsetPercentY = antiRecoilStrengthY,
                durationMs = 200 + Random.nextLong(100),
                jitterAmt = antiCheatJitterLevel
            )
        }
    }

    private fun stopEngine() {
        Log.d(TAG, "Stopping Automation Core...")
        isLoopActive = false
        isServiceRunning.value = false
        addLog("SYSTEM OFF: Releasing system blocks...")
        
        metricsJob?.cancel()
        metricsJob = null

        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null

        // Remove overlay widget from WindowManager
        floatingView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing floating overlay view: ${e.message}")
            }
        }
        floatingView = null
    }

    private fun setupFloatingControlWidget() {
        if (floatingView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }

        // Inflates simulated layout dynamically (FrameLayout wrapper)
        val frame = FrameLayout(this)
        
        val background = GradientDrawable().apply {
            setColor(Color.parseColor("#E6212121")) // Dark premium overlay
            cornerRadius = 24f
            setStroke(2, Color.parseColor("#44FF3D00")) // Orange neon visual boundary
        }
        frame.background = background
        frame.setPadding(16, 16, 16, 16)

        // Vertical stats listing
        val linear = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val titleView = TextView(this).apply {
            text = "AGAA CONTROLLER"
            setTextColor(Color.parseColor("#FFCC00"))
            textSize = 11f
            setPadding(0, 0, 0, 8)
            gravity = Gravity.CENTER
        }
        linear.addView(titleView)

        val txtFps = TextView(this).apply {
            text = "CV STATE: ACTIVE"
            setTextColor(Color.GREEN)
            textSize = 10f
        }
        linear.addView(txtFps)

        // Compact Toggle button inside widget
        val btnStop = Button(this).apply {
            text = "DEACTIVATE"
            setTextColor(Color.WHITE)
            textSize = 9f
            setPadding(8, 4, 8, 4)
            val btnDrawable = GradientDrawable().apply {
                setColor(Color.RED)
                cornerRadius = 12f
            }
            setBackground(btnDrawable)
        }
        
        btnStop.setOnClickListener {
            addLog("WIDGET MANUAL TOGGLE: Shutdown requested...")
            stopEngine()
            stopSelf()
        }
        linear.addView(btnStop)

        frame.addView(linear)
        floatingView = frame

        // Dragging gesture tracking on widget to enable moving across screen margins
        frame.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager?.updateViewLayout(v, params)
                        return true
                    }
                }
                return false
            }
        })

        try {
            windowManager?.addView(floatingView, params)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mount window overlay widget: ${e.message}")
            addLog("OVERLAY ERR: Floating widget overlay blocked. Grant overlay permissions.")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "AGAA Core Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AI Game Automation Agent Active")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setSubText("Neural CV Loop active")
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = buildNotification(text)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopEngine()
        serviceScope.cancel()
    }
}

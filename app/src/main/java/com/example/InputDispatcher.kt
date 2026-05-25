package com.example

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlin.random.Random

class InputDispatcher : AccessibilityService() {

    companion object {
        private const val TAG = "InputDispatcher"
        
        @Volatile
        private var instance: InputDispatcher? = null

        fun getInstance(): InputDispatcher? = instance
        
        fun isServiceEnabled(): Boolean = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "InputDispatcher Accessibility Service Connected.")
        instance = this
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "InputDispatcher Accessibility Service Unbound.")
        instance = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Core service handles gestures passively, no need to parse general window events
    }

    override fun onInterrupt() {
        Log.d(TAG, "InputDispatcher Service Interrupted.")
    }

    /**
     * Executes a click at the specified coordinates with anti-cheat jittering.
     * @param x Coordinate X
     * @param y Coordinate Y
     * @param jitterAmt Dynamic range in pixels to randomly shift coordinates. Low = 2-4px, High = 8-15px
     */
    fun performClick(x: Float, y: Float, jitterAmt: Float) {
        val finalX = x + ((Random.nextFloat() * 2 - 1) * jitterAmt)
        val finalY = y + ((Random.nextFloat() * 2 - 1) * jitterAmt)
        
        Log.d(TAG, "Executing Tap: ($x, $y) -> Jittered to: ($finalX, $finalY)")

        val path = Path().apply {
            moveTo(finalX, finalY)
        }

        val stroke = GestureDescription.StrokeDescription(
            path,
            10 + Random.nextLong(10), // Randomized tiny delay before tap
            50 + Random.nextLong(30)  // Randomized hold duration (ms)
        )

        val builder = GestureDescription.Builder().apply {
            addStroke(stroke)
        }

        dispatchGesture(builder.build(), object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                Log.d(TAG, "Tap gesture successfully dispatched.")
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                Log.e(TAG, "Tap gesture cancelled by OS.")
            }
        }, null)
    }

    /**
     * Simulates pull-down swipe for anti-recoil compensation
     * Includes simulated organic fingertip friction/jittering
     */
    fun performSwipeForRecoil(startX: Float, startY: Float, offsetPercentY: Float, durationMs: Long, jitterAmt: Float) {
        // Recoil pull vector: vertical Y movement downwards
        val deltaY = offsetPercentY * 100f // Scaling pulls
        val endX = startX + ((Random.nextFloat() * 2 - 1) * jitterAmt)
        val endY = startY + deltaY + ((Random.nextFloat() * 2 - 1) * jitterAmt)

        Log.d(TAG, "Executing Recoil Swipe: ($startX, $startY) to ($endX, $endY) over ${durationMs}ms")

        val path = Path()
        path.moveTo(startX, startY)

        // Generate spline interpolation to cheat neural anti-cheat systems
        val controlPoints = 3
        var prevX = startX
        var prevY = startY
        for (i in 1..controlPoints) {
            val t = i.toFloat() / controlPoints
            // Interpolate coordinates with randomized micro-jitter curves (sine wave noise)
            val interpX = startX + (endX - startX) * t + (Math.sin(t * Math.PI).toFloat() * JitterOffsetMultiplier(jitterAmt))
            val interpY = startY + (endY - startY) * t
            
            path.lineTo(interpX, interpY)
            prevX = interpX
            prevY = interpY
        }

        val stroke = GestureDescription.StrokeDescription(
            path,
            Random.nextLong(5), // Minimal latency input
            durationMs
        )

        val builder = GestureDescription.Builder().apply {
            addStroke(stroke)
        }

        dispatchGesture(builder.build(), null, null)
    }

    private fun JitterOffsetMultiplier(jitter: Float): Float {
        return (Random.nextFloat() * 2 - 1) * (jitter / 2f)
    }
}

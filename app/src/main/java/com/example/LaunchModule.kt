package com.example

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast

data class GameTarget(
    val id: String,
    val name: String,
    val packageName: String,
    val iconResName: String, // String representation for simplified loading
    val isInstalled: Boolean = false
)

object LaunchModule {

    val TARGET_GAMES = listOf(
        GameTarget("pubg_global", "PUBG Mobile (Global)", "com.tencent.ig", "ic_pubg"),
        GameTarget("pubg_india", "BGMI (PUBG India)", "com.pubg.imobile", "ic_bgmi"),
        GameTarget("free_fire", "Free Fire", "com.dts.freefireth", "ic_freefire"),
        GameTarget("cod_mobile", "Call of Duty: Mobile", "com.activision.callofduty.shooter", "ic_codm"),
        GameTarget("custom_test", "AGAA Internal Tester", "com.example", "ic_launcher_foreground")
    )

    fun getInstalledGames(context: Context): List<GameTarget> {
        val pm = context.packageManager
        return TARGET_GAMES.map { game ->
            val isInstalled = try {
                pm.getPackageInfo(game.packageName, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
            game.copy(isInstalled = isInstalled)
        }
    }

    fun launchGame(context: Context, gamePackageName: String, onSuccess: () -> Unit = {}) {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(gamePackageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            onSuccess()
        } else {
            Toast.makeText(context, "Game could not be launched. Package not found!", Toast.LENGTH_SHORT).show()
        }
    }
}

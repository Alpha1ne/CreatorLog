package com.voiceofmelody.songdailytracker.util

import android.content.Context
import android.util.Log
import android.widget.Toast

fun openInstagramApp(context: Context) {
    Log.d("Instagram", "openInstagramApp() called")
    val pm = context.packageManager
    val packages = listOf("com.instagram.android", "com.instagram.lite")
    
    var intentLaunched = false
    for (packageName in packages) {
        val launchIntent = pm.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            Log.d("Instagram", "Success: Launching $packageName")
            context.startActivity(launchIntent)
            intentLaunched = true
            break
        }
    }
    
    if (!intentLaunched) {
        Log.d("Instagram", "Failure: Instagram not installed")
    }
}

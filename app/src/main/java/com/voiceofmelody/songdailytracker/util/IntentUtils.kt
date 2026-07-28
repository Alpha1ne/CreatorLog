package com.voiceofmelody.songdailytracker.util

import android.content.Context
import android.content.Intent
import android.net.Uri
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

fun openContentLink(context: Context, url: String) {
    if (url.isBlank()) return
    
    val uri = Uri.parse(url)
    val instagramIntent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.instagram.android")
    }

    try {
        context.startActivity(instagramIntent)
    } catch (e: Exception) {
        // Fallback to browser
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(browserIntent)
        } catch (e2: Exception) {
            Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
        }
    }
}

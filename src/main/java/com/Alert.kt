package com

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.app.AlertDialog

object Alert {
    fun openSettings(context: Context) {
        if (!isServiceEnabled(context)) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Alert")
            builder.setMessage("Please enable app in settings! Otherwise it will stop working")
            builder.setPositiveButton(
                "Settings"
            ) { dialogInterface: DialogInterface?, i: Int ->
                context.startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                )
            }
            builder.show()
        }
    }

    private fun isServiceEnabled(context: Context): Boolean {
        val accessibilityManager =
            context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        if (accessibilityManager != null) {
            // Iterate over all enabled accessibility services to check if your service is enabled
            for (serviceInfo in accessibilityManager.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            )) {
                if (serviceInfo.id == context.packageName + "/" + MyAccessibilityService::class.java.name) {
                    return true
                }
            }
        }
        return false
    }
}

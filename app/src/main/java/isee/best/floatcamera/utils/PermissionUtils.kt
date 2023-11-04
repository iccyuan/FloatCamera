package isee.best.floatcamera.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log

object PermissionUtils {

    /**
     * Open the notification settings for the app.
     * Call this method if you want to let the user enable/disable notification for your app.
     */
    fun openAppNotificationSettings(context: Context) {
        val intent = Intent().apply {
            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("PermissionUtils", "Failed to open app notification settings.", e)
        }
    }

    /**
     * Open the system settings to allow the user to enable/disable the overlay permission for the app.
     * Call this method if you want to let the user enable/disable overlay for your app.
     */
    fun openOverlaySettings(context: Context) {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("PermissionUtils", "Failed to open overlay settings.", e)
        }
    }
}

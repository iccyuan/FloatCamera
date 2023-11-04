package isee.best.floatcamera.utils

import android.content.Context
import android.content.SharedPreferences

object PreferencesUtil {
    private const val PREFS_NAME = "FloatingCameraPrefs"
    private const val KEY_SERVICE_ACTIVE = "service_active"
    private const val KEY_OPACITY = "opacity"
    private const val KEY_CAMERA_DISABLED = "camera_disabled"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun setServiceActive(context: Context, isActive: Boolean) {
        getPreferences(context).edit().putBoolean(KEY_SERVICE_ACTIVE, isActive).apply()
    }

    fun isServiceActive(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_SERVICE_ACTIVE, false)
    }

    fun setOpacity(context: Context, opacity: Int) {
        getPreferences(context).edit().putInt(KEY_OPACITY, opacity).apply()
    }

    fun getOpacity(context: Context): Int {
        return getPreferences(context).getInt(KEY_OPACITY, 100)
    }

    fun setCameraDisabled(context: Context, isDisabled: Boolean) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean(KEY_CAMERA_DISABLED, isDisabled).apply()
    }

    fun isCameraDisabled(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(KEY_CAMERA_DISABLED, false)
    }
}

package isee.best.floatcamera.utils

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager

object ScreenUtil {

    // 获取屏幕宽度
    fun getScreenWidth(context: Context): Int {
        val metrics = DisplayMetrics()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getMetrics(metrics)
        return metrics.widthPixels
    }

    // 获取屏幕高度
    fun getScreenHeight(context: Context): Int {
        val metrics = DisplayMetrics()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getMetrics(metrics)
        return metrics.heightPixels
    }

    // 获取状态栏高度
    fun getStatusBarHeight(context: Context): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }
}

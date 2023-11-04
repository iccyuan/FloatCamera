package isee.best.floatcamera

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import isee.best.floatcamera.service.FloatingCameraService
import isee.best.floatcamera.utils.PreferencesUtil

class QuickSettingsTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile
        tile.state = if (isMyServiceRunning(FloatingCameraService::class.java)) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile
        val serviceIntent = Intent(this, FloatingCameraService::class.java)
        if (tile.state == Tile.STATE_INACTIVE) {
            PreferencesUtil.setServiceActive(this, true)
            // 启动服务
            startForegroundService(serviceIntent)
            tile.state = Tile.STATE_ACTIVE
        } else {
            PreferencesUtil.setServiceActive(this, false)
            // 停止服务
            stopService(serviceIntent)
            tile.state = Tile.STATE_INACTIVE
        }
        tile.updateTile()
    }

    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}

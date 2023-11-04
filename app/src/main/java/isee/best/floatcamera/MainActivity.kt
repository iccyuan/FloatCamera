package isee.best.floatcamera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Switch
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import isee.best.floatcamera.service.FloatingCameraService
import isee.best.floatcamera.utils.PermissionUtils
import isee.best.floatcamera.utils.PreferencesUtil

class MainActivity : ComponentActivity() {
    private lateinit var switch: Switch
    private lateinit var seekBar: SeekBar

    // 使用新的ActivityResult API来请求权限
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            PermissionUtils.openAppNotificationSettings(this)
            PermissionUtils.openOverlaySettings(this)
            // 权限被授予，启动服务
            startCameraServiceWithOpacity()
        } else {
            // 权限被拒绝，关闭开关
            switch.isChecked = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        switch = findViewById(R.id.sw_switch)
        seekBar = findViewById(R.id.seekBar)
        switch.setOnCheckedChangeListener { _, isChecked ->
            PreferencesUtil.setServiceActive(this, isChecked)
            if (isChecked) {
                checkPermissionAndStartService()
            } else {
                stopService(Intent(this, FloatingCameraService::class.java))
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    PreferencesUtil.setOpacity(this@MainActivity, it.progress)
                    val intent = Intent(this@MainActivity, FloatingCameraService::class.java)
                    intent.putExtra("OPACITY", it.progress)
                    startForegroundService(intent)
                }
            }
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    override fun onResume() {
        super.onResume()
        // 设置开关和透明度的初始状态
        switch.isChecked = PreferencesUtil.isServiceActive(this)
        seekBar.progress = PreferencesUtil.getOpacity(this)
    }

    private fun checkPermissionAndStartService() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // 已经有权限，启动服务
                startCameraServiceWithOpacity()
            }
            else -> {
                // 请求相机权限
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startCameraServiceWithOpacity() {
        val intent = Intent(this, FloatingCameraService::class.java).apply {
            putExtra("OPACITY", seekBar.progress)
        }
        startForegroundService(intent)
    }
}

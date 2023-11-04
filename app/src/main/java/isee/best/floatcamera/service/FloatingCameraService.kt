package isee.best.floatcamera.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.os.Build
import android.os.IBinder
import android.util.Size
import android.view.*
import android.widget.FrameLayout
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import isee.best.floatcamera.MainActivity
import isee.best.floatcamera.R
import isee.best.floatcamera.utils.PreferencesUtil
import isee.best.floatcamera.utils.ScreenUtil
import java.util.*

class FloatingCameraService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: FrameLayout
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private lateinit var textureView: TextureView
    private lateinit var cameraManager: CameraManager
    private val notificationChannelId = "FLOATING_CAMERA_SERVICE_CHANNEL"
    private val notificationId = 1


    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            setupFloatingWindow()
            initializeCamera()
        } else {
            stopSelf()
        }
    }

    private fun setupFloatingWindow() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_camera_view, null) as FrameLayout
        textureView = floatingView.findViewById(R.id.camera_preview_texture)
        val screenHeight = ScreenUtil.getScreenHeight(this)
        val statusBarHeight = ScreenUtil.getStatusBarHeight(this)

        // 设置悬浮窗参数
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            screenHeight + statusBarHeight,//直接写MATCH_PARENT在一加8上面不行，不会悬浮在状态栏上面。应该有兼容性问题
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, // 确保使用正确的Overlay类型
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, // 这将允许点击事件穿透悬浮窗
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0

            // 确保悬浮窗不会遮挡状态栏或导航栏
            flags = flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        }

        windowManager.addView(floatingView, params)
    }

    private fun initializeCamera() {
        val cameraId = cameraManager.cameraIdList[0]

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                startPreview()
            }

            override fun onDisconnected(camera: CameraDevice) {
                camera.close()
                PreferencesUtil.setServiceActive(applicationContext, false)
            }

            override fun onError(camera: CameraDevice, error: Int) {
                camera.close()
                cameraDevice = null
                PreferencesUtil.setServiceActive(applicationContext, false)
            }
        }, null)
    }

    private fun startPreview() {
        val surfaceTexture = textureView.surfaceTexture

        val characteristics = cameraManager.getCameraCharacteristics(cameraDevice?.id ?: return)
        val configurationMap = characteristics.get(
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
        )
        val previewSize = configurationMap?.getOutputSizes(SurfaceTexture::class.java)?.let {
            chooseOptimalSize(it, textureView.width, textureView.height)
        } ?: return

        surfaceTexture?.setDefaultBufferSize(previewSize.width, previewSize.height)
        val surface = Surface(surfaceTexture)

        val previewRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)?.apply {
            addTarget(surface)
        }

        val outputConfigurations = listOf(OutputConfiguration(surface))

        val sessionConfiguration = SessionConfiguration(
            SessionConfiguration.SESSION_REGULAR,
            outputConfigurations,
            mainExecutor,
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    previewRequestBuilder?.let {
                        session.setRepeatingRequest(it.build(), null, null)
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    // Handle configuration failure
                }
            })

        cameraDevice?.createCaptureSession(sessionConfiguration)
    }

    private fun chooseOptimalSize(choices: Array<Size>, textureViewWidth: Int, textureViewHeight: Int): Size {
        // 将屏幕宽高比与摄像头支持的尺寸进行比较，选择最合适的尺寸
        val aspectRatio = textureViewWidth.toDouble() / textureViewHeight
        val bigEnough = choices.filter {
            it.height == (it.width * aspectRatio).toInt()
        }
        // 如果有合适的尺寸，则选择最小的那个，否则选择最接近的尺寸
        return if (bigEnough.isNotEmpty()) {
            Collections.min(bigEnough, compareBy { it.width * it.height })
        } else {
            choices[0]
        }
    }



    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val opacity = intent.getIntExtra("OPACITY", 100)
        adjustOpacity(opacity)
        startForegroundService()
        return START_STICKY
    }

    private fun adjustOpacity(opacity: Int) {
        val params = floatingView.layoutParams as WindowManager.LayoutParams
        params.alpha = opacity / 100f
        windowManager.updateViewLayout(floatingView, params)
    }

    private fun startForegroundService() {
        val serviceChannel = NotificationChannel(
            notificationChannelId,
            getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_HIGH
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_MUTABLE)

        val notification = NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_message))
            .setSmallIcon(R.drawable.icon_camera)
            .setContentIntent(pendingIntent)
            .build()
        //展示通知栏
        manager.notify(notificationId, notification);
        startForeground(notificationId, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        captureSession?.close()
        captureSession = null
        cameraDevice?.close()
        cameraDevice = null
        if (this::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
    }
}

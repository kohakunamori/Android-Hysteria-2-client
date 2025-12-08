package us.leaf3stones.hy2droid.proxy

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import us.leaf3stones.hy2droid.R
import us.leaf3stones.hy2droid.data.KEY_IS_VPN_CONFIG_READY
import us.leaf3stones.hy2droid.data.KEY_VPN_CONFIG_PATH
import us.leaf3stones.hy2droid.data.TUN2SOCKS_CONFIG_FILE_NAME
import us.leaf3stones.hy2droid.data.vpnPrefDataStore
import us.leaf3stones.hy2droid.proxy.LogManager
import java.io.File
import java.util.Scanner
import kotlin.concurrent.thread

class Hysteria2VpnService : VpnService() {
    private external fun startTun2socks(configPath: String, fd: Int)
    private external fun stopTun2socks()
    private external fun getTun2socksStats(): LongArray

    private var netFileDescriptor: ParcelFileDescriptor? = null
    private var hysteriaProcess: Process? = null
    private var hysteriaLoggingThread: Thread? = null

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val isStart = intent?.action == ACTION_START_VPN
        val isStop = intent?.action == ACTION_STOP_VPN

        if (isStart && netFileDescriptor != null) {
            // do not respond when vpn is already started
            LogManager.warn("VPN already started, ignoring duplicate start request")
            return START_STICKY
        } else if (!isStart && !isStop) {
            Log.w(TAG, "can't recognize the intent $intent")
            LogManager.error("Unrecognized intent: $intent")
            return START_NOT_STICKY
        }

        if (isStart) {
            LogManager.log("Starting Hysteria 2 VPN service...")
            startForeground()
            scope.launch {
                val pref = vpnPrefDataStore.data.first()
                val isReady = pref[KEY_IS_VPN_CONFIG_READY] ?: false
                val configPath = pref[KEY_VPN_CONFIG_PATH] ?: ""
                startVpnChecked(isReady, configPath)
            }
            return START_STICKY;
        } else {
            LogManager.log("Stopping Hysteria 2 VPN service...")
            cleanup()
            stopSelf()
            return START_NOT_STICKY
        }
    }

    private fun startForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel.
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val mChannel = NotificationChannel("CHANNEL_ID", "name", importance)
            mChannel.description = "description"
            // Register the channel with the system. You can't change the importance
            // or other notification behaviors after this.
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }
        val notification = NotificationCompat.Builder(this, "CHANNEL_ID")
            .setContentTitle("Hy2droid running")
            .setContentText("It's time to explore the uncensored Internet!")
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSmallIcon(R.drawable.ic_vpn_connection)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
        ServiceCompat.startForeground(
            /* service = */ this,
            /* id = */ 100,
            /* notification = */ notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            },
        )
    }

    private fun startVpnChecked(isConfigReady: Boolean, configPath: String) {
        if (!isConfigReady || configPath.isBlank()) {
            Log.w(TAG, "---BUG--- vpn config not ready but start is called ---BUG---")
            LogManager.error("VPN config not ready, cannot start")
            return
        }
        Log.d(TAG, "starting hysteria, config located at $configPath")
        LogManager.log("Starting Hysteria with config: $configPath")
        
        // Log the config content for debugging
        try {
            val configContent = File(configPath).readText()
            LogManager.debug("=== Hysteria Config Start ===")
            configContent.lines().take(50).forEach { line ->
                LogManager.debug(line)
            }
            LogManager.debug("=== Hysteria Config End ===")
        } catch (e: Exception) {
            LogManager.error("Failed to read config: ${e.message}")
        }
        
        startHysteriaInternal(configPath)
        val fd = establishSystemVpnTunnel()
        startTun2socks(File(filesDir, TUN2SOCKS_CONFIG_FILE_NAME).absolutePath, fd)
        Log.d(TAG, getTun2socksStats().contentToString())
        LogManager.log("VPN tunnel established successfully")

        vpnRunning = true
        observers.forEach {
            it.onVpnStarted()
        }
    }

    private fun startHysteriaInternal(hysteriaConfig: String) {
        val commands = Array(3) { "" }
        commands[0] = File(applicationInfo.nativeLibraryDir, "libhysteria.so").absolutePath
        commands[1] = "-c"
        commands[2] = hysteriaConfig
        LogManager.debug("Executing: ${commands.joinToString(" ")}")
        hysteriaProcess = Runtime.getRuntime().exec(commands)
        hysteriaLoggingThread = thread {
            hysteriaProcess!!.errorStream.use {
                val hysteriaOutput = Scanner(it)
                while (hysteriaOutput.hasNextLine()) {
                    val nextLog = hysteriaOutput.nextLine()
                    Log.v(HYSTERIA_TAG, nextLog)
                    // Parse and categorize Hysteria logs
                    when {
                        nextLog.contains("ERROR", ignoreCase = true) || 
                        nextLog.contains("FATAL", ignoreCase = true) -> 
                            LogManager.error("[Hysteria] $nextLog")
                        nextLog.contains("WARN", ignoreCase = true) -> 
                            LogManager.warn("[Hysteria] $nextLog")
                        nextLog.contains("DEBUG", ignoreCase = true) -> 
                            LogManager.debug("[Hysteria] $nextLog")
                        else -> 
                            LogManager.verbose("[Hysteria] $nextLog")
                    }
                    if (Thread.interrupted()) {
                        break
                    }
                }
            }
        }
    }

    private fun establishSystemVpnTunnel(): Int {
        netFileDescriptor = Builder().setMtu(1500).addAddress("10.0.88.88", 16)
            .addDnsServer("1.0.0.1").addDisallowedApplication(packageName)
            .addRoute("0.0.0.0", 0).establish()
        return netFileDescriptor!!.fd
    }

    private fun cleanup() {
        try {
            LogManager.log("Cleaning up VPN resources...")
            vpnRunning = false
            observers.forEach {
                it.onVpnStopped()
            }
            observers.clear()
            stopTun2socks()
            netFileDescriptor?.close()
            netFileDescriptor = null
            hysteriaProcess?.destroy()
            hysteriaProcess = null
            hysteriaLoggingThread?.interrupt()
            hysteriaLoggingThread = null
            LogManager.log("VPN stopped successfully")
        } catch (e: Exception) {
            LogManager.error("Error during cleanup: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        cleanup()
    }

    companion object {
        init {
            try {
                System.loadLibrary("tun2socks")
            } catch (unsatisfied: UnsatisfiedLinkError) {
                throw RuntimeException(unsatisfied)
            }
        }

        const val ACTION_START_VPN =
            "us.leaf3stones.hy2droid.proxy.Hysteria2VpnService.ACTION_START_VPN"
        const val ACTION_STOP_VPN =
            "us.leaf3stones.hy2droid.proxy.Hysteria2VpnService.ACTION_STOP_VPN"

        interface VpnStatusObserver {
            fun onVpnStarted()
            fun onVpnStopped()
        }

        fun addObserver(observer: VpnStatusObserver) {
            observers.add(observer)
        }

        fun removeObserver(observer: VpnStatusObserver) {
            if (!observers.remove(observer)) {
                Log.w(TAG, "trying to remove a non-exist observer $observer")
            }
        }

        private val TAG = Hysteria2VpnService::class.java.simpleName.toString()
        private const val HYSTERIA_TAG = "hysteria"
        private val observers: MutableSet<VpnStatusObserver> = mutableSetOf()
        
        @Volatile
        private var vpnRunning = false
        
        /**
         * Check if VPN is currently running
         * @return true if VPN service is active, false otherwise
         */
        fun isVpnRunning(): Boolean = vpnRunning
    }
}
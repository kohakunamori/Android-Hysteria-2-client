package us.leaf3stones.hy2droid.tile

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import us.leaf3stones.hy2droid.proxy.Hysteria2VpnService
import us.leaf3stones.hy2droid.ui.activities.MainActivity

/**
 * Quick Settings Tile for Hysteria 2 VPN
 * Allows users to quickly toggle VPN from the control center
 */
@RequiresApi(Build.VERSION_CODES.N)
class Hysteria2VpnTileService : TileService() {
    
    private var isVpnConnected = false
    
    private val vpnStatusObserver = object : Hysteria2VpnService.Companion.VpnStatusObserver {
        override fun onVpnStarted() {
            isVpnConnected = true
            updateTile()
        }

        override fun onVpnStopped() {
            isVpnConnected = false
            updateTile()
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Hysteria2VpnService.addObserver(vpnStatusObserver)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Hysteria2VpnService.removeObserver(vpnStatusObserver)
    }
    
    override fun onStartListening() {
        super.onStartListening()
        // Sync with actual VPN state when tile becomes visible
        isVpnConnected = Hysteria2VpnService.isVpnRunning()
        updateTile()
    }
    
    override fun onClick() {
        super.onClick()
        
        // If VPN is not connected, open MainActivity to configure
        if (!isVpnConnected) {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                startActivityAndCollapse(intent)
            }
        } else {
            // Stop VPN
            val stopIntent = Intent(this, Hysteria2VpnService::class.java).apply {
                action = Hysteria2VpnService.ACTION_STOP_VPN
            }
            startService(stopIntent)
        }
    }
    
    private fun updateTile() {
        qsTile?.apply {
            state = if (isVpnConnected) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = "Hysteria 2"
            subtitle = if (isVpnConnected) "已连接" else "未连接"
            
            // Update icon (you can customize this)
            contentDescription = if (isVpnConnected) {
                "Hysteria 2 VPN 已连接，点击断开"
            } else {
                "Hysteria 2 VPN 未连接，点击打开应用"
            }
            
            updateTile()
        }
    }
}

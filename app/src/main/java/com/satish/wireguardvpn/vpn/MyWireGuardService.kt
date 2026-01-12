package com.satish.wireguardvpn.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.satish.wireguardvpn.MainActivity
import com.satish.wireguardvpn.R
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import com.wireguard.config.InetEndpoint
import com.wireguard.config.InetNetwork
import com.wireguard.config.Peer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MyWireGuardService : VpnService() {

    companion object {
        private const val NOTIF_CHANNEL = "vpn"
        private const val NOTIF_ID = 42
        private const val NOTIF_CHANNEL_NAME = "Status da VPN"
        private const val TAG = "MyWireGuardService"
        const val ACTION_START_TUNNEL = "com.satish.vpn.action.START_TUNNEL"
    }

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var tunInterface: ParcelFileDescriptor? = null
    private var backend: GoBackend? = null
    private val tunnel: Tunnel = WireGuardTunnel()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TUNNEL -> {
                VpnStateRepository.setRunning(true)
                startTunnel()
            }
            else -> stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy chamado")
        scope.launch {
            try {
                VpnStateRepository.setRunning(false)
                backend?.setState(tunnel, Tunnel.State.DOWN, null)
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao desligar backend", e)
            }
        }
        
        try {
            tunInterface?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao fechar interface TUN", e)
        }
        
        tunInterface = null
        backend = null
        scope.cancel()
        super.onDestroy()
    }

    private fun startTunnel() {
        ensureNotification()

        try {
            val config = createWireGuardConfig()
            backend = GoBackend(this)

            val builder = Builder()
                .setSession("VPN PlayVicioRP")
                .setMtu(1340)
                .addAddress("10.66.66.2", 32)
                .addDnsServer("1.1.1.1")
                .addRoute("178.132.198.227", 32)

            scope.launch(Dispatchers.IO) {
                try {
                    tunInterface = builder.establish()
                    
                    if (tunInterface == null) {
                        Log.e(TAG, "Falha ao estabelecer interface TUN")
                        VpnStateRepository.setRunning(false)
                        stopSelf()
                        return@launch
                    }
                    
                    Log.i(TAG, "Interface TUN estabelecida com sucesso")
                    backend?.setState(tunnel, Tunnel.State.UP, config)
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao iniciar túnel VPN", e)
                    VpnStateRepository.setRunning(false)
                    stopSelf()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao configurar VPN", e)
            VpnStateRepository.setRunning(false)
            stopSelf()
        }
    }

    private fun createWireGuardConfig(): Config {
        return Config.Builder()
            .setInterface(
                com.wireguard.config.Interface.Builder()
                    .parsePrivateKey("ABWdhTXtVK3LROm08HP95ydFeri+zPumhbXIImXMr28=")
                    .addAddress(InetNetwork.parse("10.66.66.2/32"))
                    .addDnsServer(java.net.InetAddress.getByName("1.1.1.1"))
                    .build()
            )
            .addPeer(
                Peer.Builder()
                    .parsePublicKey("FTGjvPTXq1MbY7pSlHMGAWXzp+BL/L3Cux5ubow9OXQ=")
                    .setEndpoint(InetEndpoint.parse("54.232.157.183:51820"))
                    .addAllowedIp(InetNetwork.parse("178.132.198.227/32"))
                    .setPersistentKeepalive(25)
                    .build()
            )
            .build()
    }

    private fun ensureNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
            ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIF_CHANNEL,
                NOTIF_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notif: Notification = NotificationCompat.Builder(this, NOTIF_CHANNEL)
            .setContentTitle("VPN PlayVicioRP")
            .setContentText("Conectado")
            .setSmallIcon(R.drawable.outline_admin_panel_settings_24)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
            
        startForeground(NOTIF_ID, notif)
    }

    private inner class WireGuardTunnel : Tunnel {
        override fun getName() = "PlayVicioRP"

        override fun onStateChange(newState: Tunnel.State) {
            Log.d(TAG, "Estado do túnel alterado para: $newState")
            if (newState == Tunnel.State.DOWN) {
                stopSelf()
            }
        }
    }
}

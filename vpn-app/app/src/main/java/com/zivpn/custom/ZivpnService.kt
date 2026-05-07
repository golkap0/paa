package com.zivpn.custom

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ZivpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val processes = mutableListOf<Process>()
    private val TAG = "ZivpnService"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "START") {
            startVpn()
        } else if (intent?.action == "STOP") {
            stopVpn()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startVpn() {
        Log.i(TAG, "Starting VPN")
        createNotificationChannel()
        startForeground(1, createNotification())

        val prefs = getSharedPreferences("vpn_prefs", Context.MODE_PRIVATE)
        val config = VpnConfig(
            serverHost = prefs.getString("server", "") ?: "",
            authUser = prefs.getString("auth", "") ?: "",
            obfsKey = prefs.getString("obfs", "") ?: "",
            cores = prefs.getInt("cores", 4),
            upLimit = prefs.getString("up", "1 Mbps") ?: "1 Mbps",
            downLimit = prefs.getString("down", "1 Mbps") ?: "1 Mbps",
            dns = prefs.getString("dns", "8.8.8.8,1.1.1.1") ?: "8.8.8.8,1.1.1.1"
        )

        try {
            startNativeProcesses(config)
            establishVpn(config)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start VPN", e)
            stopVpn()
        }
    }

    private fun startNativeProcesses(config: VpnConfig) {
        val nativeDir = applicationInfo.nativeLibraryDir
        val logFile = File(cacheDir, "load.log")

        val tunnels = mutableListOf<String>()
        val coreCount = config.cores.coerceIn(1, 10)

        for (i in 0 until coreCount) {
            val port = 1080 + i
            val jsonConfig = JSONObject().apply {
                put("server", "${config.serverHost}:6000-19999")
                put("obfs", config.obfsKey)
                put("auth", config.authUser)
                put("socks5", JSONObject().put("listen", "127.0.0.1:$port"))
                put("insecure", true)

                val up = config.upLimit.trim().lowercase()
                if (up != "0" && up != "0 mbps" && up.isNotEmpty()) {
                    put("up", config.upLimit)
                }
                val down = config.downLimit.trim().lowercase()
                if (down != "0" && down != "0 mbps" && down.isNotEmpty()) {
                    put("down", config.downLimit)
                }
                put("recvwindowconn", config.recvWinConn)
                put("recvwindow", config.recvWin)
            }

            val pb = ProcessBuilder(
                "$nativeDir/libuz.so",
                "-s", config.obfsKey,
                "--config", jsonConfig.toString()
            )
            pb.directory(cacheDir)
            pb.redirectErrorStream(true)
            val process = pb.start()
            processes.add(process)
            tunnels.add("127.0.0.1:$port")
            Log.i(TAG, "Started libuz on port $port")
        }

        // Start libload
        val lbArgs = mutableListOf("$nativeDir/libload.so", "-lhost", "127.0.0.1", "-lport", "7777", "-tunnel")
        lbArgs.addAll(tunnels)
        val lbPb = ProcessBuilder(lbArgs)
        lbPb.directory(cacheDir)
        lbPb.redirectErrorStream(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            lbPb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile))
        }
        val lbProcess = lbPb.start()
        processes.add(lbProcess)
        Log.i(TAG, "Started libload on port 7777")
    }

    private fun establishVpn(config: VpnConfig) {
        val builder = Builder()
            .setSession("Zivpn Custom")
            .setMtu(1500)
            .addAddress("10.0.0.2", 24)
            .addRoute("0.0.0.0", 0)

        config.dns.split(",").forEach {
            val d = it.trim()
            if (d.isNotEmpty()) builder.addDnsServer(d)
        }

        vpnInterface = builder.establish()
        Log.i(TAG, "VPN Interface established")

        // Start tun2socks bridge
        startTun2Socks()
    }

    private fun startTun2Socks() {
        val nativeDir = applicationInfo.nativeLibraryDir
        val tunFd = vpnInterface?.fd ?: return

        val tsPb = ProcessBuilder(
            "$nativeDir/libtun2socks.so",
            "-device", "fd://$tunFd",
            "-proxy", "socks5://127.0.0.1:7777",
            "-interface", "tun0"
        )
        tsPb.directory(cacheDir)
        tsPb.redirectErrorStream(true)
        try {
            val process = tsPb.start()
            processes.add(process)
            Log.i(TAG, "Started tun2socks bridge")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start tun2socks", e)
        }
    }

    private fun stopVpn() {
        Log.i(TAG, "Stopping VPN")
        processes.forEach { it.destroy() }
        processes.clear()
        vpnInterface?.close()
        vpnInterface = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "vpn_channel",
                "VPN Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, "vpn_channel")
        } else {
            Notification.Builder(this)
        }

        return builder
            .setContentTitle("Zivpn Custom")
            .setContentText("VPN is running...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpn()
    }
}

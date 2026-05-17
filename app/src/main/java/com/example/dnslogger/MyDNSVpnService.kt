package com.example.dnslogger

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream

class MyDNSVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var isRunning = false
    private var captureThread: Thread? = null

    private fun sendLogToUI(logLine: String) {
        val intent = Intent("DNS_DATA_SIGNAL")
        intent.putExtra("log_message", logLine)
        sendBroadcast(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_VPN") {
            stopVpn()
            return START_NOT_STICKY
        }
        if (isRunning) return START_STICKY
        isRunning = true

        try {
            vpnInterface = Builder()
                .setSession("DNSLoggerEngine")
                .addAddress("10.0.0.1", 24)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("8.8.8.8")
                .establish()

            sendLogToUI("SYSTEM: Engine Active")
            startPacketCapture()

        } catch (e: Exception) {
            sendLogToUI("ERROR: Initialization Failed: ${e.message}")
        }

        return START_STICKY
    }

    private fun startPacketCapture() {
        captureThread = Thread {
            try {
                val fd = vpnInterface?.fileDescriptor ?: return@Thread
                val inputStream = FileInputStream(fd)
                val packet = ByteArray(32767)

                while (isRunning && !Thread.currentThread().isInterrupted) {
                    val length = inputStream.read(packet)
                    if (length > 20) {
                        val version = (packet[0].toInt() shr 4) and 0x0F
                        if (version == 4) {
                            val srcIp = "${packet[12].toUByte()}.${packet[13].toUByte()}.${packet[14].toUByte()}.${packet[15].toUByte()}"
                            val destIp = "${packet[16].toUByte()}.${packet[17].toUByte()}.${packet[18].toUByte()}.${packet[19].toUByte()}"

                            if (destIp != "10.0.0.1") {
                                sendLogToUI("$srcIp -> $destIp")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (isRunning) {
                    Log.e("ENGINE", "Capture error", e)
                }
            }
        }
        captureThread?.start()
    }

    private fun stopVpn() {
        isRunning = false
        captureThread?.interrupt()
        vpnInterface?.close()
        vpnInterface = null
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }
}
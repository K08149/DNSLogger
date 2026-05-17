package com.example.dnslogger

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private val logList = mutableStateListOf<String>()
    private var isServiceActive by mutableStateOf(false)

    private val vpnReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val message = intent?.getStringExtra("log_message") ?: return
            logList.add("[$getTimestamp] $message")
        }
    }

    private val vpnLaunchResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startVpnServiceInstance()
        } else {
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ContextCompat.registerReceiver(
            this,
            vpnReceiver,
            IntentFilter("DNS_DATA_SIGNAL"),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        setContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF121212))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DNS Logger",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Packets logged: ${logList.size}",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    items(logList) { log ->
                        Text(
                            text = log,
                            color = Color(0xFF00AA00),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp)
                        .background(Color(0xFF1A1A1A), shape = RoundedCornerShape(16.dp))
                        .padding(8.dp)
                        .height(50.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { toggleVpnServiceState() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isServiceActive) Color(0xFFCC0000) else Color(0xFF00FF7F)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxHeight()
                    ) {
                        Text(
                            text = if (isServiceActive) "STOP LOGGING" else "START LOGGING",
                            color = if (isServiceActive) Color.White else Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = { logList.clear() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2E2E)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .weight(0.6f)
                            .fillMaxHeight()
                    ) {
                        Text(
                            text = "CLEAR",
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = { downloadLogsToStorage() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0055AA)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .weight(0.5f)
                            .fillMaxHeight()
                    ) {
                        Canvas(modifier = Modifier.size(20.dp)) {
                            val arrowWidth = size.width
                            val arrowHeight = size.height

                            val path = Path().apply {
                                moveTo(arrowWidth / 2f, 0f)
                                lineTo(arrowWidth / 2f, arrowHeight * 0.75f)
                                moveTo(arrowWidth * 0.2f, arrowHeight * 0.45f)
                                lineTo(arrowWidth / 2f, arrowHeight * 0.75f)
                                lineTo(arrowWidth * 0.8f, arrowHeight * 0.45f)
                                moveTo(0f, arrowHeight)
                                lineTo(arrowWidth, arrowHeight)
                            }

                            drawPath(
                                path = path,
                                color = Color.White,
                                style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun toggleVpnServiceState() {
        if (isServiceActive) {
            val stopIntent = Intent(this, MyDNSVpnService::class.java).apply { action = "STOP_VPN" }
            startService(stopIntent)
            isServiceActive = false
            logList.add("[$getTimestamp] SYSTEM: Engine Terminated Manually")
        } else {
            val vpnIntent = VpnService.prepare(this)
            if (vpnIntent != null) {
                vpnLaunchResult.launch(vpnIntent)
            } else {
                startVpnServiceInstance()
            }
        }
    }

    private fun startVpnServiceInstance() {
        val startIntent = Intent(this, MyDNSVpnService::class.java)
        startService(startIntent)
        isServiceActive = true
    }

    private fun downloadLogsToStorage() {
        if (logList.isEmpty()) {
            Toast.makeText(this, "Console output is empty", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val defaultFileName = "DNSLogger_${System.currentTimeMillis()}.txt"
            val downloadFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val exportTargetFile = File(downloadFolder, defaultFileName)

            FileOutputStream(exportTargetFile).use { out ->
                logList.forEach { logLine ->
                    out.write((logLine + "\n").toByteArray())
                }
            }

            Toast.makeText(this, "Saved to Downloads folder", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private val getTimestamp: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

    override fun onDestroy() {
        unregisterReceiver(vpnReceiver)
        super.onDestroy()
    }
}
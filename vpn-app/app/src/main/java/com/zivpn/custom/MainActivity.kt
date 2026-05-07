package com.zivpn.custom

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var etServer: EditText
    private lateinit var etAuth: EditText
    private lateinit var etObfs: EditText
    private lateinit var tvCores: TextView
    private lateinit var sbCores: SeekBar
    private lateinit var etUpLimit: EditText
    private lateinit var etDownLimit: EditText
    private lateinit var etDns: EditText
    private lateinit var etImportExport: EditText
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnImport: Button
    private lateinit var btnExport: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        loadConfig()
        setupListeners()
    }

    private fun initViews() {
        etServer = findViewById(R.id.etServer)
        etAuth = findViewById(R.id.etAuth)
        etObfs = findViewById(R.id.etObfs)
        tvCores = findViewById(R.id.tvCores)
        sbCores = findViewById(R.id.sbCores)
        etUpLimit = findViewById(R.id.etUpLimit)
        etDownLimit = findViewById(R.id.etDownLimit)
        etDns = findViewById(R.id.etDns)
        etImportExport = findViewById(R.id.etImportExport)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        btnImport = findViewById(R.id.btnImport)
        btnExport = findViewById(R.id.btnExport)

        sbCores.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val cores = if (progress < 1) 1 else progress
                tvCores.text = "Cores: $cores"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupListeners() {
        btnStart.setOnClickListener {
            val intent = VpnService.prepare(this)
            if (intent != null) {
                startActivityForResult(intent, 0)
            } else {
                onActivityResult(0, RESULT_OK, null)
            }
        }

        btnStop.setOnClickListener {
            stopVpn()
        }

        btnImport.setOnClickListener {
            val input = etImportExport.text.toString()
            val configs = ConfigUtils.parseMultiLineImport(input)
            if (configs.isNotEmpty()) {
                val first = configs[0]
                etServer.setText(first.serverHost)
                etAuth.setText(first.authUser)
                Toast.makeText(this, "Imported ${configs.size} accounts. Loaded the first one.", Toast.LENGTH_SHORT).show()
                saveConfig()
            } else {
                Toast.makeText(this, "Invalid format", Toast.LENGTH_SHORT).show()
            }
        }

        btnExport.setOnClickListener {
            val config = getCurrentConfig()
            val url = ConfigUtils.serializeConfig(config)
            etImportExport.setText(url)
            Toast.makeText(this, "Config exported", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCurrentConfig(): VpnConfig {
        return VpnConfig(
            serverHost = etServer.text.toString(),
            authUser = etAuth.text.toString(),
            obfsKey = etObfs.text.toString(),
            cores = if (sbCores.progress < 1) 1 else sbCores.progress,
            upLimit = etUpLimit.text.toString(),
            downLimit = etDownLimit.text.toString(),
            dns = etDns.text.toString()
        )
    }

    private fun saveConfig() {
        val config = getCurrentConfig()
        val prefs = getSharedPreferences("vpn_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("server", config.serverHost)
            putString("auth", config.authUser)
            putString("obfs", config.obfsKey)
            putInt("cores", config.cores)
            putString("up", config.upLimit)
            putString("down", config.downLimit)
            putString("dns", config.dns)
            apply()
        }
    }

    private fun loadConfig() {
        val prefs = getSharedPreferences("vpn_prefs", Context.MODE_PRIVATE)
        etServer.setText(prefs.getString("server", "ssh-2.chice.me"))
        etAuth.setText(prefs.getString("auth", "vpnstunnel-bnml0"))
        etObfs.setText(prefs.getString("obfs", "hu``hqb`c"))
        val cores = prefs.getInt("cores", 4)
        sbCores.progress = cores
        tvCores.text = "Cores: $cores"
        etUpLimit.setText(prefs.getString("up", "1 Mbps"))
        etDownLimit.setText(prefs.getString("down", "1 Mbps"))
        etDns.setText(prefs.getString("dns", "8.8.8.8,1.1.1.1"))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            saveConfig()
            startVpn()
        }
    }

    private fun startVpn() {
        val intent = Intent(this, ZivpnService::class.java).apply {
            action = "START"
        }
        startService(intent)
    }

    private fun stopVpn() {
        val intent = Intent(this, ZivpnService::class.java).apply {
            action = "STOP"
        }
        startService(intent)
    }
}

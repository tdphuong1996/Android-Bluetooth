package com.nsc9012.bluetooth.devices

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothProfile.ServiceListener
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.widget.Toast
import com.nsc9012.bluetooth.R
import com.nsc9012.bluetooth.extension.hasPermission
import com.nsc9012.bluetooth.extension.invisible
import com.nsc9012.bluetooth.extension.toast
import com.nsc9012.bluetooth.extension.visible
import kotlinx.android.synthetic.main.activity_devices.*
import java.io.IOException


class DevicesActivity : AppCompatActivity() {

    companion object {
        const val ENABLE_BLUETOOTH = 1
        const val REQUEST_ENABLE_DISCOVERY = 2
        const val REQUEST_ACCESS_COARSE_LOCATION = 3
    }

    /* Broadcast receiver to listen for discovery results. */
    private val bluetoothDiscoveryResult = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothDevice.ACTION_FOUND) {
                val device: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!

                deviceListAdapter.addDevice(device)
            }
        }
    }

    /* Broadcast receiver to listen for discovery updates. */
    private val bluetoothDiscoveryMonitor = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    progress_bar.visible()
                    toast("Scan started...")
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    progress_bar.invisible()
                    toast("Scan complete. Found ${deviceListAdapter.itemCount} devices.")
                }
            }
        }
    }

    private val bluetoothPair = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent!!.action
            if (action == BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED) {
                val state =
                    intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, BluetoothA2dp.STATE_DISCONNECTED)
                if (state == BluetoothA2dp.STATE_CONNECTED) {
                    setIsA2dpReady(true)
                    progress_bar.invisible()

//                    playMusic()
                } else if (state == BluetoothA2dp.STATE_DISCONNECTED) {
                    setIsA2dpReady(false)
                }
            } else if (action == BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED) {
                val state =
                    intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, BluetoothA2dp.STATE_NOT_PLAYING)
                if (state == BluetoothA2dp.STATE_PLAYING) {
                } else {
                }
            }
        }
    }
    var mIsA2dpReady = false
    fun setIsA2dpReady(ready: Boolean) {
        mIsA2dpReady = ready
        Toast.makeText(this, "A2DP ready ? " + if (ready) "true" else "false", Toast.LENGTH_SHORT)
            .show()
    }

    private val mA2dpListener: ServiceListener = object : ServiceListener {
        override fun onServiceConnected(profile: Int, a2dp: BluetoothProfile) {
            if (profile == BluetoothProfile.A2DP) {
                mA2dpService = a2dp as BluetoothA2dp
//                if (mAudioManager!!.isBluetoothA2dpOn) {
//                    setIsA2dpReady(true)
//
//                } else {
//                }
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            setIsA2dpReady(false)
        }
    }

    private fun playMusic() {
    }


    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private val deviceListAdapter = DevicesAdapter()
    var mAudioManager: AudioManager? = null
    var mA2dpService: BluetoothA2dp? = null

    private var isRecord: Boolean = false
    private var outputFile: String = ""
    private  var myAudioRecorder= MediaRecorder()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_devices)
        initUI()
        deviceListAdapter.onItemClick={
            progress_bar.visible()
            it.createBond()
        }

        if ( hasPermission(Manifest.permission.RECORD_AUDIO) && hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            outputFile=
                Environment.getExternalStorageDirectory().getAbsolutePath().toString() + "/"+System.currentTimeMillis()+"recording.3gp"

            myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            myAudioRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB)
            myAudioRecorder.setOutputFile(outputFile)
        }else{
            ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO),
           50
        )}

        bindEvent()
        btnPlay.invisible()


    }

    private fun bindEvent() {
        tvRecord.setOnClickListener {
            if (!isRecord) {
                isRecord = true
                tvRecord.text = "Dừng ghi âm"
                try {

                    myAudioRecorder.prepare()
                    myAudioRecorder.start()
                    Toast.makeText(this, "Bắt đầu ghi âm", Toast.LENGTH_LONG)
                        .show()
                } catch (ise: IllegalStateException) {
                    // make something ...
                } catch (ioe: IOException) {
                    // make something
                }
                btnPlay.visible()
                btnPlay.isEnabled = false


            } else {
                isRecord = false
                tvRecord.text = "Ghi âm"
                myAudioRecorder.stop()
                myAudioRecorder.release()
                btnPlay.isEnabled = true


            }

            btnPlay.setOnClickListener {

                try {
                    val mediaPlayer = MediaPlayer()
                    mediaPlayer.setDataSource(outputFile)
                    mediaPlayer.prepare()
                    mediaPlayer.start()
                    Toast.makeText(applicationContext, "Playing Audio", Toast.LENGTH_LONG)
                        .show()
                } catch (e: Exception) {
                    // make something
                    Toast.makeText(this, e.message, Toast.LENGTH_LONG)
                        .show()
                }

            }

        }
    }

    private fun initUI() {
        title = "Bluetooth Scanner"
        recycler_view_devices.adapter = deviceListAdapter
        recycler_view_devices.layoutManager = LinearLayoutManager(this)
        recycler_view_devices.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        button_discover.setOnClickListener { initBluetooth() }
    }

    private fun initBluetooth() {

        if (bluetoothAdapter.isDiscovering) return

        if (bluetoothAdapter.isEnabled) {
            enableDiscovery()
        } else {
            // Bluetooth isn't enabled - prompt user to turn it on
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(intent, ENABLE_BLUETOOTH)
        }
//        bluetoothAdapter.getProfileProxy(this, mA2dpListener , BluetoothProfile.A2DP)

    }

    private fun enableDiscovery() {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        startActivityForResult(intent, REQUEST_ENABLE_DISCOVERY)
    }

    private fun monitorDiscovery() {
        registerReceiver(bluetoothDiscoveryMonitor, IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED))
        registerReceiver(bluetoothDiscoveryMonitor, IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
        registerReceiver(bluetoothPair, IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED))
        registerReceiver(bluetoothPair, IntentFilter(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED))
    }

    private fun startDiscovery() {
        if (hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION) && hasPermission(Manifest.permission.RECORD_AUDIO) && hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            if (bluetoothAdapter.isEnabled && !bluetoothAdapter.isDiscovering) {
                beginDiscovery()
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO),
                REQUEST_ACCESS_COARSE_LOCATION
            )
        }
    }

    private fun beginDiscovery() {
        registerReceiver(bluetoothDiscoveryResult, IntentFilter(BluetoothDevice.ACTION_FOUND))
        deviceListAdapter.clearDevices()
        monitorDiscovery()
        bluetoothAdapter.startDiscovery()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_ACCESS_COARSE_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    beginDiscovery()
                }
                else {
                    toast("Permission required to scan for devices.")
                }

            }
            50->{
                outputFile=
                Environment.getExternalStorageDirectory().getAbsolutePath().toString() + "/"+System.currentTimeMillis()+"recording.3gp"

                myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                myAudioRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB)
                myAudioRecorder.setOutputFile(outputFile)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            ENABLE_BLUETOOTH -> if (resultCode == Activity.RESULT_OK) {
                enableDiscovery()
            }
            REQUEST_ENABLE_DISCOVERY -> if (resultCode == Activity.RESULT_CANCELED) {
                toast("Discovery cancelled.")
            } else {
                startDiscovery()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothDiscoveryMonitor)
        unregisterReceiver(bluetoothDiscoveryResult)
    }


}

package com.sanicorporation.bluetoothchat.activity

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.*
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.sanicorporation.bluetoothchat.R
import com.sanicorporation.bluetoothchat.adapter.ChatAdapter
import com.sanicorporation.bluetoothchat.adapter.DeviceAdapter
import com.sanicorporation.bluetoothchat.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    val viewModel: MainViewModel by viewModels()


    lateinit var adapter: DeviceAdapter
    lateinit var dialog: AlertDialog

    companion object {
        const val FINE_LOCATION_REQUEST = 200
        const val REQUEST_ENABLE_BT = 201
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        setUpBinding()
        registerReceiver(discoveryStateReceiver, IntentFilter(ACTION_SCAN_MODE_CHANGED))
        registerReceiver(newDevicesReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        checkPermissions()

    }

    private fun setUpBinding() {
        binding.lifecycleOwner = this
        binding.viewmodel = viewModel
        binding.activity = this
        binding.messagesRecycler.let {
            it.adapter = viewModel.chatAdapter
            val obsever = object : RecyclerView.AdapterDataObserver(){
                override fun onChanged() {
                    it.layoutManager!!.scrollToPosition(viewModel.chatAdapter.getLastPosition())
                }
            }

            it.adapter!!.registerAdapterDataObserver(obsever)
        }
        binding.connectBluetoothButton.setOnClickListener {
            openDeviceDialog()
        }
        binding.sendTextButton.setOnClickListener {
            viewModel.sendMessage()
        }
        binding.btBecomeVisibleButton.setOnClickListener{
            requestBtVisibility()
        }


    }

    private fun openDeviceDialog() {
        val deviceList = arrayListOf<BluetoothDevice>()
        getDefaultAdapter().bondedDevices!!.forEach {
            deviceList.add(it)
        }
        adapter = DeviceAdapter(this,deviceList){
            getDefaultAdapter().cancelDiscovery()
            dialog.dismiss()
            viewModel.connectWithThisDevice(it)
        }
        val builder = AlertDialog.Builder(this)
        dialog = builder
                .setAdapter(adapter,null)
                .setTitle(R.string.title_pairing_dialog)
                .setNeutralButton(R.string.discover_new_devices,null)
                .show()

        val neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
        neutralButton.setOnClickListener{ getDefaultAdapter().startDiscovery() }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            continueAfterPermissionsAccepted()
        } else {
            requestNeededPermissions()
        }
    }

    private fun requestNeededPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
            showExplanation()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),FINE_LOCATION_REQUEST)

        }
    }

    private fun showExplanation() {
        TODO("Not yet implemented")
    }

    private fun continueAfterPermissionsAccepted() {
        if(BluetoothAdapter.getDefaultAdapter().isEnabled){
            continueAfterBtEnabled()
        } else {
            enableBluetooth()
        }
    }

    private fun enableBluetooth() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
    }


    private fun continueAfterBtEnabled() {
        viewModel.startListeningIncomes()
        requestBtVisibility()
    }

    private fun requestBtVisibility() {
        if (BluetoothAdapter.getDefaultAdapter().scanMode != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE){
            val discoverableIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            }
            startActivity(discoverableIntent)
        } else {
            viewModel.setIsDeviceVisible(true)
        }

    }

    private fun updateList(device: BluetoothDevice?) {
        device?.let {
            adapter.add(device)
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == FINE_LOCATION_REQUEST){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                continueAfterPermissionsAccepted()
            } else {
                showExplanation()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT){
            if(resultCode == RESULT_OK){
                continueAfterBtEnabled()
            } else {
                Toast.makeText(this, "You don't have your Bluetooth enabled", Toast.LENGTH_LONG).show()
            }
        }
    }

    private val discoveryStateReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            when(action){
                ACTION_SCAN_MODE_CHANGED -> {
                    when(intent.getIntExtra(EXTRA_SCAN_MODE,0)){
                        SCAN_MODE_CONNECTABLE ->{
                            viewModel.setIsDeviceVisible(false)
                        }
                        SCAN_MODE_CONNECTABLE_DISCOVERABLE -> {
                            viewModel.setIsDeviceVisible(true)
                        }
                        SCAN_MODE_NONE -> {
                            viewModel.setIsDeviceVisible(false)
                        }
                    }

                }
            }

        }
    }

    private val newDevicesReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            when(action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    updateList(device)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(discoveryStateReceiver)
        unregisterReceiver(newDevicesReceiver)
    }
}

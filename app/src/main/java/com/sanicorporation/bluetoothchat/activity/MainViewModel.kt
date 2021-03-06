package com.sanicorporation.bluetoothchat.activity

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.databinding.Bindable
import androidx.databinding.BindingAdapter
import androidx.lifecycle.*
import androidx.recyclerview.widget.RecyclerView
import com.sanicorporation.BluetoothChat
import com.sanicorporation.bluetoothchat.adapter.ChatAdapter
import com.sanicorporation.bluetoothchat.model.From
import com.sanicorporation.bluetoothchat.model.Message
import kotlinx.coroutines.*
import java.io.IOException
import java.util.*
import java.util.Observer


class MainViewModel: ViewModel() {

    private val _pairingSocket: MutableLiveData<BluetoothSocket> = MutableLiveData()
    private val _sendTextEnabled: MutableLiveData<Boolean> = MutableLiveData(false)
    private val _isDeviceVisible: MutableLiveData<Boolean> = MutableLiveData(false)
    private val _connectedDevice: MutableLiveData<Boolean> = MutableLiveData()


    val message : MutableLiveData<String> = MutableLiveData()
    val chatAdapter= ChatAdapter()

    private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
        BluetoothAdapter.getDefaultAdapter()?.listenUsingInsecureRfcommWithServiceRecord(BluetoothChat.packageNameForService, uuid)
    }

    companion object {
        const val TAG = "Bluetooth Server"
        private val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    }


    fun getSendTextEnabled() : LiveData<Boolean> = _sendTextEnabled

    fun getIsDeviceVisible() : LiveData<Boolean> = _isDeviceVisible

    fun setIsDeviceVisible(boolean: Boolean){
        _isDeviceVisible.postValue(boolean)
    }

    fun getPairingSocket():LiveData<BluetoothSocket> = _pairingSocket

    fun getConnectedDevice() = _connectedDevice

    fun startListeningIncomes(){
        viewModelScope.launch {
            try {
                startServer().await()?.let {
                    _connectedDevice.postValue(true)
                    _pairingSocket.value=it
                    listenAndSendToRecycler()
                }
            } catch (exception: Exception){

            }
        }
    }

    fun connectWithThisDevice(device: BluetoothDevice){
        viewModelScope.launch {
            try {
                connectToDevice(device).await()?.let {
                    _connectedDevice.postValue(true)
                    _pairingSocket.value=it
                    listenAndSendToRecycler()
                }
            } catch (exception: Exception){

            }

        }
    }

    private suspend fun startServer() = GlobalScope.async(SupervisorJob() + Dispatchers.IO) {

        var socket: BluetoothSocket? = null
        var shouldLoop = true
        while (shouldLoop) {
            socket = try {
                mmServerSocket?.accept()
            } catch (e: IOException) {
                Log.e(TAG, "Socket's accept() method failed", e)
                shouldLoop = false
                null
            }
            socket?.also {
                mmServerSocket?.close()
                shouldLoop = false
            }

        }
        socket
    }


    private suspend fun connectToDevice(device: BluetoothDevice)= GlobalScope.async(SupervisorJob() + Dispatchers.IO){
        mmServerSocket?.close()
        val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createInsecureRfcommSocketToServiceRecord(uuid)
        }
        mmSocket?.connect()
        mmSocket
    }




        val mmBuffer: ByteArray = ByteArray(1024)

        fun sendMessage(message:String) = GlobalScope.launch{
            val output = _pairingSocket.value!!.outputStream
            try {
                output.write(message.toByteArray())
                withContext(Dispatchers.Main){
                    chatAdapter.addMessage(Message(message,From.ME))
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error occurred when sending data", e)
                _connectedDevice.postValue(false)
                // Send a failure message back to the activity.
            }
        }

        fun listenAndSendToRecycler() = GlobalScope.launch{
            var numBytes = 0
            val input = _pairingSocket.value!!.inputStream

            while (true) {
                // Read from the InputStream.
                numBytes = try {
                    input.read(mmBuffer)
                } catch (e: IOException) {
                    Log.d(TAG, "Input stream was disconnected", e)
                    _connectedDevice.postValue(false)
                    break
                }
                val readMessage = String(mmBuffer, 0, numBytes);
                withContext(Dispatchers.Main){
                    chatAdapter.addMessage(Message(readMessage, From.OTHER))
                }

            }
        }


    fun sendMessage() {
        message.value?.let {
            viewModelScope.launch {
                sendMessage(it)
            }
            message.postValue("")
        }
    }
}
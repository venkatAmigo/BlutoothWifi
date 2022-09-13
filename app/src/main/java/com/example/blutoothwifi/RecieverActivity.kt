package com.example.blutoothwifi

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.blutoothwifi.databinding.ActivityRecieverBinding
import java.io.IOException
import java.io.InputStream
import java.util.*

class RecieverActivity : AppCompatActivity() {
    private lateinit var bluetoothDiscoveryLauncher: ActivityResultLauncher<Intent>
    private val REQUEST_CODE: Int = 100
    private var bluetoothAdapter: BluetoothAdapter? = null
    lateinit var bluetoothManager: BluetoothManager

    lateinit var binding: ActivityRecieverBinding
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecieverBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter


        bluetoothDiscoveryLauncher = registerForActivityResult(
            ActivityResultContracts
            .StartActivityForResult()){
            if(it.resultCode == RESULT_OK){
                Toast.makeText(this, "Discovery Enabled", Toast.LENGTH_SHORT).show()
            }

        }
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED

            ) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission
                    .BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION,Manifest
                    .permission.ACCESS_COARSE_LOCATION,Manifest
                    .permission
                    .BLUETOOTH_SCAN
                ),REQUEST_CODE)
            }else{
                bluetoothSetup()
            }
    }

    private fun bluetoothSetup() {
        val bluetoothDiscoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,500)
        }
        bluetoothDiscoveryLauncher.launch(bluetoothDiscoverableIntent)
        AcceptThread().start()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == REQUEST_CODE ) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[2] ==
                PackageManager.PERMISSION_GRANTED) {
                bluetoothSetup()
            } else {
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this, Manifest.permission
                        .BLUETOOTH_CONNECT
                )
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this, Manifest.permission
                        .BLUETOOTH_SCAN
                )
            }
            if (grantResults[1] != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this, Manifest.permission
                        .BLUETOOTH_SCAN
                )
            }
        }
    }
    @SuppressLint("MissingPermission")
    private fun serverStub(){
        val uuid = UUID.fromString("38ec8b11-da4d-4ee7-813a-f0d4eb014ac6")
        Thread {
            bluetoothAdapter?.startDiscovery()
            val server : BluetoothServerSocket? = bluetoothAdapter?.listenUsingRfcommWithServiceRecord("Bluetooth", uuid)
            var loop = true
            while (loop) {
                Log.d("Server", "Printing")
                val bluetoothSocket: BluetoothSocket? = try {
                    server?.accept()
                } catch (e: IOException) {
                    Log.e("ServerSocket", "Socket's accept() failed", e)
                    e.printStackTrace()
                    null
                }
                bluetoothSocket?.let {
                    //binder.socketConnectedCallback?.onSocketConnected(it)
                }
                server?.close()
                loop = false
            }
        }.start()
    }
//    @SuppressLint("MissingPermission")
//    private inner class AcceptThread : Thread() {
//
//        private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
//            bluetoothAdapter?.listenUsingRfcommWithServiceRecord("iQOO 7", UUID.fromString
//                ("81bff704-335a-11ed-a261-0242ac120002"))
//        }
//
//        override fun run() {
//            // Keep listening until exception occurs or a socket is returned.
//            var shouldLoop = true
//            while (shouldLoop) {
//                Handler(mainLooper).post {
//                    binding.textView.text = "hello"
//                    Toast.makeText(this@RecieverActivity, "hell o", Toast
//                        .LENGTH_SHORT)
//                        .show()
//                }
//                try {
//                    Thread{
//                        val socket: BluetoothSocket? =  mmServerSocket?.accept()
//                        socket?.also {
//                            //manageMyConnectedSocket(it)
//                            Thread {
//                                val buffer = ByteArray(1024)
//                                val mmInStream: InputStream = it.inputStream
//                                val text: String = mmInStream.read(buffer).toString()
//                                Handler(mainLooper).post {
//                                    binding.textView.text = text
//                                }
//                            }.start()
//                        }
//                        socket?.close()
//                    }.start()
//
//                } catch (e: IOException) {
//
//                    Log.e("", "Socket's accept() method failed", e)
//                    shouldLoop = false
//                    null
//                }
//
//
//                    mmServerSocket?.close()
//                    shouldLoop = false
//                }
//            }
//
//        // Closes the connect socket and causes the thread to finish.
//        fun cancel() {
//            try {
//                mmServerSocket?.close()
//            } catch (e: IOException) {
//                //Log.e(TAG, "Could not close the connect socket", e)
//            }
//        }
//    }
}
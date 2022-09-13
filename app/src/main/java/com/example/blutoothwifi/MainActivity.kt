package com.example.blutoothwifi

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.blutoothwifi.databinding.ActivityMainBinding
import java.io.IOException
import java.io.OutputStream
import java.util.*


class MainActivity : AppCompatActivity() {
    private lateinit var receiver: BroadcastReceiver
    private lateinit var bluetoothDiscoveryLauncher: ActivityResultLauncher<Intent>
    private val REQUEST_CODE: Int = 100
    private var bluetoothAdapter: BluetoothAdapter? = null
    lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothEnableLauncher: ActivityResultLauncher<Intent>

    lateinit var binding: ActivityMainBinding
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter
        if(bluetoothAdapter == null){
            Toast.makeText(this, "Bluetooth Not supported", Toast.LENGTH_SHORT).show()
        }
        bluetoothEnableLauncher = registerForActivityResult(ActivityResultContracts
            .StartActivityForResult()){
            if(it.resultCode == RESULT_OK){
                getPairedDevices()
                discoverDevices()
            }

        }
        bluetoothDiscoveryLauncher = registerForActivityResult(ActivityResultContracts
            .StartActivityForResult()){
            if(it.resultCode == RESULT_OK){
                Toast.makeText(this, "Discovery Enabled", Toast.LENGTH_SHORT).show()
            }

        }
        if(bluetoothAdapter?.isEnabled == false){
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

                return
            }else{
                bluetoothSetup()
            }
        }else{
            bluetoothSetup()
        }


    }

    @SuppressLint("MissingPermission")
    private fun discoverDevices(){
        receiver =object: BroadcastReceiver(){
            override fun onReceive(p0: Context?, intent: Intent?) {
                if(intent?.action == BluetoothDevice.ACTION_FOUND){
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    Handler(mainLooper).post {
                        Toast.makeText(this@MainActivity, "Found device ${device?.name} ", Toast
                            .LENGTH_SHORT)
                            .show()
                    }
                    if (device != null && device.name == "iQOO 7") {
                        val conThread = ConnectThread(device)
                        conThread.start()
                    }
                }
            }

        }
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver,filter)
        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter?.cancelDiscovery();
        }
        if(bluetoothAdapter?.startDiscovery() == true){
            Toast.makeText(this, "Discovery Started", Toast.LENGTH_SHORT).show()
        }else{
            Toast.makeText(this, "Discovery not Started", Toast.LENGTH_SHORT).show()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }
    @SuppressLint("MissingPermission")
    private fun getPairedDevices() {
        val pairedDevices = bluetoothAdapter?.bondedDevices
        val devices = mutableListOf<String>()
        pairedDevices?.forEach {
            devices.add(it.name)
        }
        binding.devices.adapter = ArrayAdapter(this,android.R.layout
            .simple_spinner_dropdown_item,devices)
        binding.devices.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, index: Int, p3: Long) {

            }

            override fun onNothingSelected(p0: AdapterView<*>?) {

            }

        }
    }

    @SuppressLint("MissingPermission")
    private fun bluetoothSetup(){
        if(bluetoothAdapter?.isEnabled == true){
            bluetoothAdapter?.name = "Venkat"
            getPairedDevices()
            discoverDevices()
        }else {
            bluetoothAdapter?.name = "Venkat"
            val bluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            bluetoothEnableLauncher.launch(bluetoothIntent)
        }
        val bluetoothDiscoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,200)
        }
        bluetoothDiscoveryLauncher.launch(bluetoothDiscoverableIntent)
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
    private  inner class ConnectThread(val device: BluetoothDevice): Thread(){

        private val bluSocket : BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE){
                //device.createRfcommSocketToServiceRecord(device.uuids[0].uuid)
            //static uuid
            device.createRfcommSocketToServiceRecord(UUID.fromString("81bff704-335a-11ed-a261-0242ac120002"))
        }
        override fun run() {
            super.run()
            Handler(mainLooper).post {
                Toast.makeText(this@MainActivity, "device ${device.name}", Toast.LENGTH_SHORT).show()
            }
            bluetoothAdapter?.cancelDiscovery()
            bluSocket.let {
                try{
                    if (it != null) {
                        try {
                            it.close()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    it?.connect()
                }catch (e:Exception){
                    try {
                        Log.e("", "trying fallback...")
                        val socket = device.javaClass.getMethod(
                            "createRfcommSocket",
                            *arrayOf<Class<*>?>(Int::class.javaPrimitiveType)
                        ).invoke(device, 2) as BluetoothSocket
                        socket.connect()
                        Thread{
                            val mmOutStream: OutputStream = socket.outputStream
                            try {
                                mmOutStream.write("Hello siddu".toByteArray())
                                mmOutStream.flush()
                                mmOutStream.close()
                                //val values = ContentValues()
                               /* val CONTENT_URI: Uri =
                                    Uri.parse("content://com.android.bluetooth.opp/btopp")
                                values.put("uri", "file:///sdcard/refresh.txt")
                                values.put("direction", device.address)
                                values.put("destination",0
                                )
                                val ts = System.currentTimeMillis()
                                values.put("timestamp", ts)
                                contentResolver.insert(CONTENT_URI, values)*/
                                //socket.close()
                            } catch (e: IOException) {

                            }
                        }.start()
                        Log.e("", "Connected")
                    } catch (e2: java.lang.Exception) {
                        Log.e("", "Couldn't establish Bluetooth connection!")
                    }
                    Log.i("CONNCT",e.localizedMessage)

                }

            }

        }
        fun cancel(){
            try {
                bluSocket?.close()
            }catch (e:IOException){
                Log.e("IOE","Couldn't close application")
            }

        }
    }
}
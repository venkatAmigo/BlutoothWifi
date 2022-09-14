package com.example.blutoothwifi

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class ImportedActivity : AppCompatActivity() {
    lateinit var listen: Button
    lateinit var send: Button
    lateinit var listDevices: Button
    lateinit var listView: ListView
    lateinit var msg_box: TextView
    lateinit var status: TextView
    lateinit var writeMsg: EditText

    lateinit var bluetoothAdapter: BluetoothAdapter
    private var btArray: Array<BluetoothDevice?>? = null

    var sendReceive: SendReceive? = null

    val STATE_LISTENING = 1
    val STATE_CONNECTING = 2
    val STATE_CONNECTED = 3
    val STATE_CONNECTION_FAILED = 4
    val STATE_MESSAGE_RECEIVED = 5

    private val REQUEST_CODE: Int = 100

    var REQUEST_ENABLE_BLUETOOTH = 1

    private val APP_NAME = "BTChat"
    private val MY_UUID = UUID.fromString("8ce255c0-223a-11e0-ac64-0803450c9a66")

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_imported)
        findViewByIdes()
        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter
        val bluetoothEnableLauncher = registerForActivityResult(
            ActivityResultContracts
                .StartActivityForResult()
        ) {
            if (it.resultCode == RESULT_OK) {
                askPermissions()
            }

        }
        if (!bluetoothAdapter.isEnabled) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            bluetoothEnableLauncher.launch(enableIntent)
        }

        askPermissions()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun askPermissions() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED

        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission
                        .BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION, Manifest
                        .permission.ACCESS_COARSE_LOCATION, Manifest
                        .permission
                        .BLUETOOTH_SCAN
                ), REQUEST_CODE
            )

            return
        } else {
            implementListeners()
        }
    }

    @SuppressLint("MissingPermission")
    private fun implementListeners() {
        listDevices.setOnClickListener(View.OnClickListener {
            val bt: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
            val strings = arrayOfNulls<String>(bt.size)
            btArray = arrayOfNulls(bt.size)
            var index = 0
            if (bt.isNotEmpty()) {
                for (device in bt) {
                    btArray!![index] = device
                    strings[index] = device.name
                    index++
                }
                val arrayAdapter = ArrayAdapter(
                    applicationContext, android.R.layout.simple_list_item_1, strings
                )
                listView.adapter = arrayAdapter
            }
        })
        listen.setOnClickListener(View.OnClickListener {
            val serverClass = ServerClass()
            serverClass.start()
        })
        listView.onItemClickListener = OnItemClickListener { adapterView, view, i, l ->
            val clientClass = btArray?.get(i)?.let { ClientClass(it) }
            clientClass?.start()
            status.setText("Connecting")
        }
        send.setOnClickListener(View.OnClickListener {
            val string: String = writeMsg.text.toString()
            sendReceive?.write(string.toByteArray())
        })
    }

    var handler = Handler { msg ->
        when (msg.what) {
            STATE_LISTENING -> status.setText("Listening")
            STATE_CONNECTING -> status.setText("Connecting")
            STATE_CONNECTED -> status.setText("Connected")
            STATE_CONNECTION_FAILED -> status.setText("Connection Failed")
            STATE_MESSAGE_RECEIVED -> {
                val readBuff = msg.obj as ByteArray
                val tempMsg = String(readBuff, 0, msg.arg1)
                msg_box.text = tempMsg
            }
        }
        true
    }

    private fun findViewByIdes() {
        listen = findViewById<Button>(R.id.listen)
        send = findViewById<Button>(R.id.send)
        listView = findViewById<ListView>(R.id.listview)
        msg_box = findViewById<TextView>(R.id.msg)
        status = findViewById<TextView>(R.id.status)
        writeMsg = findViewById<EditText>(R.id.writemsg)
        listDevices = findViewById<Button>(R.id.listDevices)
    }

    @SuppressLint("MissingPermission")
    private inner class ServerClass : Thread() {
        private var serverSocket: BluetoothServerSocket? = null
        override fun run() {
            var socket: BluetoothSocket? = null
            while (socket == null) {
                try {
                    val message = Message.obtain()
                    message.what = STATE_CONNECTING
                    handler.sendMessage(message)
                    socket = serverSocket!!.accept()
                } catch (e: IOException) {
                    e.printStackTrace()
                    val message = Message.obtain()
                    message.what = STATE_CONNECTION_FAILED
                    handler.sendMessage(message)
                }
                if (socket != null) {
                    val message = Message.obtain()
                    message.what = STATE_CONNECTED
                    handler.sendMessage(message)
                    sendReceive = SendReceive(socket)
                    sendReceive!!.start()
                    break
                }
            }
        }

        init {
            try {
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(
                    APP_NAME,
                    MY_UUID
                )
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private inner class ClientClass(private val device: BluetoothDevice) : Thread() {
        private var socket: BluetoothSocket? = null

        @SuppressLint("MissingPermission")
        override fun run() {
            try {
                socket!!.connect()
                val message = Message.obtain()
                message.what = STATE_CONNECTED
                handler.sendMessage(message)
                sendReceive = SendReceive(socket)
                sendReceive!!.start()
            } catch (e: IOException) {
                e.printStackTrace()
                val message = Message.obtain()
                message.what = STATE_CONNECTION_FAILED
                handler.sendMessage(message)
            }
        }

        init {
            try {
                socket = device.createRfcommSocketToServiceRecord(MY_UUID)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    inner class SendReceive(private val bluetoothSocket: BluetoothSocket?) : Thread() {
        private val inputStream: InputStream?
        private val outputStream: OutputStream?
        override fun run() {
            val buffer = ByteArray(1024)
            var bytes: Int
            while (true) {
                try {
                    bytes = inputStream!!.read(buffer)
                    handler.obtainMessage(STATE_MESSAGE_RECEIVED, bytes, -1, buffer)
                        .sendToTarget()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        fun write(bytes: ByteArray?) {
            try {
                outputStream!!.write(bytes)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        init {
            var tempIn: InputStream? = null
            var tempOut: OutputStream? = null
            try {
                tempIn = bluetoothSocket!!.inputStream
                tempOut = bluetoothSocket.outputStream
            } catch (e: IOException) {
                e.printStackTrace()
            }
            inputStream = tempIn
            outputStream = tempOut
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[2] ==
                PackageManager.PERMISSION_GRANTED
            ) {
                implementListeners()
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
            if (grantResults[1] != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this, Manifest.permission
                        .BLUETOOTH_SCAN
                )
            }
        }
    }
}

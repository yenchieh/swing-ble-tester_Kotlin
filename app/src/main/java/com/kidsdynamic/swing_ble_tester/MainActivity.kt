package com.kidsdynamic.swing_ble_tester

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.AdapterView
import android.widget.ScrollView
import android.widget.Toast
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.polidea.rxandroidble.RxBleClient
import com.polidea.rxandroidble.RxBleConnection
import com.polidea.rxandroidble.RxBleDevice
import com.polidea.rxandroidble.RxBleScanResult
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.startActivityForResult
import org.json.JSONObject
import rx.Subscription
import java.util.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.support.annotation.NonNull
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat


class MainActivity : AppCompatActivity() {

    val BLUETOOTH_PERMISSION = 0x1000
    val BLUETOOTH_ADMIN_PERMISSION = 0x1001
    val PERMISSION_REQUEST_COARSE_LOCATION = 1
    val BLUETOOTH_PRIVILEGED_PERMISSION = 1

    private lateinit var scanSubscription: Subscription
    private lateinit var bleSubscription: Subscription

    private val DEVICE_NAME = "SWING"
    private val SWING_SERVICE_UUID = UUID.fromString("0000ffa0-0000-1000-8000-00805f9b34fb")
    private val ENABLE_DEVICE_UUID = UUID.fromString("0000ffa1-0000-1000-8000-00805f9b34fb")
    private val TEST_START_UUID = UUID.fromString("0000ffae-0000-1000-8000-00805f9b34fb")
    private val TIME_UUID = UUID.fromString("0000ffa3-0000-1000-8000-00805f9b34fb")
    private val MAC_UUID = UUID.fromString("0000ffa6-0000-1000-8000-00805f9b34fb")
    private val TEST_DATA_UUID = UUID.fromString("0000ffaf-0000-1000-8000-00805f9b34fb")
    private val CHOOSE_DEVICE_ACTIVITY_ID = 101
    private var macAddress: String = ""
    private lateinit var connectDevice: RxBleDevice

    private var mAutoScrolling = false
    private lateinit var mCompany: String
    private val baseUrl = "https://childrenlab.com:8110/api/final"

    private var mBluetoothAdapter: BluetoothAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainToolBar.setTitle(R.string.main_title)
        setSupportActionBar(mainToolBar)

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE doesn't support", Toast.LENGTH_LONG).show()
            finish()
        }

        // Initializes Bluetooth adapter.
        val bluetoothManager: BluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.getAdapter()

        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Start enable device", Toast.LENGTH_LONG).show()

            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, 1)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, Array(1, {android.Manifest.permission.BLUETOOTH}), BLUETOOTH_PERMISSION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, Array(1, {android.Manifest.permission.BLUETOOTH_ADMIN}), BLUETOOTH_ADMIN_PERMISSION);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, Array(1, {android.Manifest.permission.ACCESS_COARSE_LOCATION}), PERMISSION_REQUEST_COARSE_LOCATION);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_PRIVILEGED) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, Array(1, {android.Manifest.permission.BLUETOOTH_PRIVILEGED}), BLUETOOTH_PRIVILEGED_PERMISSION);
        }



        attachListener()
    }

    fun attachListener() {
        startButton.setOnClickListener { _ ->
            if (startButton.text == "START TEST") {
                runOnUiThread({ startButton.text = "STOP TEST" })
//                val intent = Intent(this, javaClass<DeviceListActivity>())
                startActivityForResult<DeviceListActivity>(CHOOSE_DEVICE_ACTIVITY_ID)
//                startBle()
            } else {
                runOnUiThread({ startButton.text = "START TEST" })
                stopBle()
            }


        }

        autoScrolling.setOnCheckedChangeListener({ _, b ->
            mAutoScrolling = b
        })

        val options = resources.getStringArray(R.array.company_array)
        company.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                mCompany = options[p2]
                updateLog("Selected: ${mCompany}")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        println("Activity Result ${requestCode} ${resultCode}")
        if(requestCode == CHOOSE_DEVICE_ACTIVITY_ID && data != null) {

            println(data.extras["macAddress"])
            macAddress = data.extras["macAddress"] as String

            connectDevice()
        }

    }

    fun startBle() {
        runOnUiThread({ startButton.text = "STOP TEST" })
        val rxBleClient = RxBleClient.create(this)
        updateLog("Scanning....")
        scanSubscription = rxBleClient.scanBleDevices()
                .subscribe { rxBleScanResult ->
                    receiveDevice(rxBleScanResult)
                }

    }

    fun stopBle() {
        runOnUiThread({ startButton.text = "START TEST" })
/*
        if (scanSubscription.isUnsubscribed) {
            scanSubscription.unsubscribe()
        }

*/


        if (bleSubscription.isUnsubscribed) {
            bleSubscription.unsubscribe()
        }
        macAddress = ""

        updateLog("Stopped test")
    }

    fun receiveDevice(rxBleScanResult: RxBleScanResult) {

/*        if (rxBleScanResult.bleDevice.name == DEVICE_NAME && bleList[rxBleScanResult.bleDevice.name] == null) {
            bleList[DEVICE_NAME] = rxBleScanResult.bleDevice
            scanSubscription.unsubscribe()
            logText.text = "${logText.text}\nSwing Device Found"
            connectDevice()

        }*/

    }

    fun connectDevice() {
        if (macAddress == "") {
            return
        }
        val rxBleClient = RxBleClient.create(this)

        connectDevice = rxBleClient.getBleDevice(macAddress)

        updateLog("Start to connecting device...")
        val subscription = connectDevice.establishConnection(this, false) // <-- autoConnect flag
                ?.subscribe(
                        { rxBleConnection ->
                            // All GATT operations are done through the rxBleConnection.
                            updateLog("Connection established to ${DEVICE_NAME}. Mac ID: ${connectDevice.macAddress}")
                            updateDeviceName(connectDevice.name)
                            discoverServices(rxBleConnection)
                            enableSwing(rxBleConnection)

                        },
                        { throwable ->
                            updateError("Connect device: ${throwable.message}")

                        }
                )

        subscription?.let {
            this.bleSubscription = subscription
        }
    }

    fun discoverServices(rxBleConnection: RxBleConnection) {
        rxBleConnection.discoverServices()
                .subscribe(
                        { services ->
                            for (service in services.bluetoothGattServices) {
                                println("Service UUID: ${service.uuid}")
                                for (character in service.characteristics) {
                                    println("Chatacter: ${character.uuid}")
                                }

                            }

                        }
                )
    }

    fun enableSwing(rxBleConnection: RxBleConnection) {
        val data = ByteArray(1)
        data[0] = 0x01
        rxBleConnection.writeCharacteristic(ENABLE_DEVICE_UUID, data)
                .subscribe(
                        { _ ->
                            updateLog("Enabled device")
                            enableTest(rxBleConnection)
                        },
                        { throwable ->
                            throwable.printStackTrace()
                            updateError("enable string: ${throwable.message}")
                        }
                )
    }

    fun enableTest(rxBleConnection: RxBleConnection) {
        updateLog("Enable test...")
        val data = ByteArray(3)
        data[0] = 0x54
        data[1] = 0x33
        data[2] = 0x45

        rxBleConnection.writeCharacteristic(TEST_START_UUID, data)
                .subscribe(
                        { _ ->
                            updateLog("Enable successfully")
//                            sendTimestamp(rxBleConnection)
                            startTest(rxBleConnection)
                        },
                        { throwable ->
                            throwable.printStackTrace()
                            updateError("enableTest: ${throwable.message}")
                        }
                )
    }

    fun startTest(rxBleConnection: RxBleConnection) {
        updateLog("Start getting data............")
        rxBleConnection.readCharacteristic(TEST_DATA_UUID)
                .subscribe(
                        { characteristicValue ->
                            if (characteristicValue == null) {
                                updateLog("Read null from test Data")
                                return@subscribe
                            }
                            updateLog("Value Length: ${characteristicValue.size}")
                            for (b in characteristicValue) {
                                updateLog("Byte: $b")
                            }
                            updateLog("---------Success-------")

                            uploadResult(connectDevice.macAddress, characteristicValue, true)
                        }
                )
    }

    fun sendTimestamp(rxBleConnection: RxBleConnection) {
        updateLog("Send time stamp")
        val unixTime = (System.currentTimeMillis() / 1000).toInt()
        updateLog("Send timestamp to: $TIME_UUID  Timestamp: $unixTime")
        val timestampBytes = longToBytes(unixTime)
        rxBleConnection.writeCharacteristic(TIME_UUID, timestampBytes)
                .subscribe(
                        { _ ->
                            updateLog("Send timestamp successfully")
                            getMacID(rxBleConnection)
                        },
                        { throwable ->
                            throwable.printStackTrace()
                            updateError(throwable.message)
                        }
                )
    }

    fun getMacID(rxBleConnection: RxBleConnection) {
        updateLog("Getting Mac ID")
        rxBleConnection.readCharacteristic(MAC_UUID)
                .subscribe(
                        { characteristicValue ->
                            val macId = bytesToHex(characteristicValue)
                            updateLog("MAC ID: $macId")
                        }
                )
    }

    fun uploadResult(macId: String?, resultData: ByteArray, success: Boolean) {
        if (macId == null) {
            updateLog("--------- Fail Upload data because MAC ID = null -------")
            return
        }
        val jsonBody = JSONObject("{\"mac_id\":\"" + macId + "\", \"x_max\":\"" + resultData[0] +
                "\", \"x_min\":\"" + resultData[1] + "\", \"y_max\":\"" + resultData[2] +
                "\", \"y_min\":\"" + resultData[3] + "\" , \"uv_max\":\"" + (resultData[4].toString() + "" + resultData[5]) +
                "\", \"uv_min\":\"" + (resultData[5].toString() + " " + resultData[6]) +
                "\", \"company\":\"" + mCompany +
                "\", \"mac_id\":\"" + macId + "\", \"result\":" + success + "}")

        val request = object : JsonObjectRequest(Request.Method.POST, baseUrl, jsonBody, Response.Listener<JSONObject> {
            //Success response
            updateLog("Upload to backend successfully")
            stopBle()
        }, Response.ErrorListener { error ->
            error.printStackTrace()
            updateLog("upload result error")
            updateError("Error on uploading test result")
            stopBle()
        }) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val params = mutableMapOf<String, String>()
                params.put("X-AUTH-TOKEN", "50ddcb9f1da9b08c2892ba58b694859e")
                return params
            }
        }
        val queue = Volley.newRequestQueue(this)
        queue.add(request)
    }

    fun updateLog(log: String) {
        runOnUiThread({ logText.text = "${logText.text}\n ${log}" })

        if (mAutoScrolling) {
            logScroll.postDelayed({ logScroll.fullScroll(ScrollView.FOCUS_DOWN) }, 500)
        }
    }

    fun updateError(err: String?) {
        runOnUiThread({ errorMessage.text = err })
    }

    fun updateDeviceName(deviceName: String?) {
        runOnUiThread({ deviceNameText.text = deviceName })
    }

    fun updateDeviceStatus(status: String?) {
        runOnUiThread({ deviceStatus.text = status })
    }

    fun longToBytes(unixTime: Int): ByteArray {
        return byteArrayOf((unixTime shr 24).toByte(), (unixTime shr 16).toByte(), (unixTime shr 8).toByte(), unixTime.toByte())
    }

    private val hexArray = "0123456789ABCDEF".toCharArray()
    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = hexArray[v.ushr(4)]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }

        val stringChars = String(hexChars)
        var newString = ""
        for (i in stringChars.indices step 2) {
            newString = "${stringChars.substring(i, i + 2)}${newString}"
        }
        return newString
    }

    override fun onRequestPermissionsResult(requestCode: Int, @NonNull permissions: Array<String>, @NonNull grantResults: IntArray) {

        when (requestCode) {
            BLUETOOTH_PERMISSION ->
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Bluetooth permission denied", Toast.LENGTH_SHORT).show();
                }

             BLUETOOTH_ADMIN_PERMISSION ->
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Bluetooth admin permission denied", Toast.LENGTH_SHORT).show();
                }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}

package com.kidsdynamic.swing_ble_tester

import android.support.v7.app.AppCompatActivity
import android.os.Bundle

import kotlinx.android.synthetic.main.activity_main.*
import com.polidea.rxandroidble.RxBleClient
import com.polidea.rxandroidble.RxBleDevice
import com.polidea.rxandroidble.RxBleScanResult
import rx.Subscription
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import com.polidea.rxandroidble.RxBleConnection
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ScrollView
import java.util.*




class MainActivity : AppCompatActivity() {

    private var bleList = mutableMapOf<String, RxBleDevice>()
    private lateinit var scanSubscription: Subscription
    private lateinit var bleSubscription: Subscription

    private val DEVICE_NAME = "SWING"
    private val SWING_SERVICE_UUID = UUID.fromString("0000ffa0-0000-1000-8000-00805f9b34fb")
    private val ENABLE_DEVICE_UUID = UUID.fromString("0000ffa1-0000-1000-8000-00805f9b34fb")
    private val TEST_START_UUID = UUID.fromString("0000ffae-0000-1000-8000-00805f9b34fb")
    private val TIME_UUID = UUID.fromString("0000ffa3-0000-1000-8000-00805f9b34fb")
    private val MAC_UUID = UUID.fromString("0000ffa6-0000-1000-8000-00805f9b34fb")
    private val TEST_DATA_UUID = UUID.fromString("0000ffaf-0000-1000-8000-00805f9b34fb")
    private var mAutoScrolling = false
    private lateinit var mCompany: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        val REQUEST_ENABLE_BT = 1
        this.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)

        attachListener()
    }

    fun attachListener() {
        startButton.setOnClickListener { v ->
            startBle()
        }

        autoScrolling.setOnCheckedChangeListener({ v, b ->
            updateLog("Checked: $b")
            mAutoScrolling = b
        })

        val options = resources.getStringArray(R.array.company_array)
        company.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                mCompany = options[p2]
                updateLog("Selected: ${mCompany}")
            }
        }
    }

    fun startBle() {
        val rxBleClient = RxBleClient.create(this)

        scanSubscription = rxBleClient.scanBleDevices()
                .subscribe { rxBleScanResult ->
                    receiveDevice(rxBleScanResult)
                }

    }

    fun receiveDevice(rxBleScanResult: RxBleScanResult) {

        if (rxBleScanResult.bleDevice.name == DEVICE_NAME && bleList[rxBleScanResult.bleDevice.name] == null) {
            bleList[DEVICE_NAME] = rxBleScanResult.bleDevice
            scanSubscription.unsubscribe()
            logText.text = "${logText.text}\nSwing Device Found"
            connectDevice()

        }

    }

    fun connectDevice() {
        if(bleList.isEmpty() || bleList[DEVICE_NAME] == null) {
            return
        }

        updateLog("Start to connecting device...")
        val subscription = bleList[DEVICE_NAME]?.establishConnection(this, false) // <-- autoConnect flag
                ?.subscribe(
                        { rxBleConnection ->

                            // All GATT operations are done through the rxBleConnection.
                            updateLog("Connection established to ${DEVICE_NAME}. Mac ID: ${bleList[DEVICE_NAME]?.macAddress}")
                            updateDeviceName(bleList[DEVICE_NAME]?.name)
                            discoverServices(rxBleConnection)
                            enableSwing(rxBleConnection)

                        },
                        { throwable ->

                            updateLog("Error occur: ${throwable.message}")
                            updateError(throwable.message)

                        }
                )
    }

    fun discoverServices(rxBleConnection: RxBleConnection) {
        rxBleConnection.discoverServices()
                .subscribe(
                        { services ->
                            for(service in services.bluetoothGattServices) {
                                println("Service UUID: ${service.uuid}")
                                for(character in service.characteristics) {
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
                        { characteristicValue ->
                            updateLog("Enabled device")
                            enableTest(rxBleConnection)
                        },
                        { throwable ->
                            throwable.printStackTrace()
                            updateError(throwable.message)
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
                        { characteristicValue ->
                            updateLog("Enable successfully")
//                            sendTimestamp(rxBleConnection)
                            startTest(rxBleConnection)
                        },
                        { throwable ->
                            throwable.printStackTrace()
                            updateError(throwable.message)
                        }
                )
    }

    fun startTest(rxBleConnection: RxBleConnection) {
        updateLog("Start getting data............")
        var subscription = rxBleConnection.readCharacteristic(TEST_DATA_UUID)
                .subscribe(
                        { characteristicValue ->
                            if(characteristicValue == null) {
                                updateLog("Read null from test Data")
                                return@subscribe
                            }
                            updateLog("Value Length: ${characteristicValue.size}")
                            for(b in characteristicValue) {
                                updateLog("Byte: $b")
                            }
                            updateLog("---------Success-------")
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
                        { characteristicValue ->
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

    fun updateLog(log: String) {
        runOnUiThread({logText.text = "${logText.text}\n ${log}"})

        if(mAutoScrolling){
            logScroll.postDelayed({ logScroll.fullScroll(ScrollView.FOCUS_DOWN) }, 500)
        }


    }

    fun updateError(err: String?) {
        runOnUiThread({errorMessage.text = err})
    }

    fun updateDeviceName(deviceName: String?) {
        runOnUiThread({ deviceNameText.text = deviceName })
    }

    fun udpateDeviceStatus(status: String?) {
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
        for(i in stringChars.indices step 2) {
            newString = "${stringChars.substring(i, i+2)}${newString}"
        }
        return newString
    }
}

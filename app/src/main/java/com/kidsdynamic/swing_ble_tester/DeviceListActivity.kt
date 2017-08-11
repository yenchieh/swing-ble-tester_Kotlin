package com.kidsdynamic.swing_ble_tester

import android.app.Activity
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity
import com.polidea.rxandroidble.RxBleClient
import com.polidea.rxandroidble.RxBleDevice
import kotlinx.android.synthetic.main.activity_device_list.*
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.setContentView
import rx.Subscription


class DeviceListActivity : AppCompatActivity() {
    private lateinit var scanSubscription: Subscription
    private lateinit var adapter : DeviceListAdapter
    private val deviceList = ArrayList<RxBleDevice>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("restart created")
        setContentView(R.layout.activity_device_list)
        adapter = DeviceListAdapter(deviceList)

        deviceListView.adapter = adapter

        startBle()
    }

    fun startBle() {
        val rxBleClient = RxBleClient.create(this)

        scanSubscription = rxBleClient.scanBleDevices()
                .subscribe { rxBleScanResult ->
                    println(rxBleScanResult.bleDevice.macAddress)
                    if(!deviceList.contains(rxBleScanResult.bleDevice)) {
                        deviceList.add(rxBleScanResult.bleDevice)
                        adapter.add(rxBleScanResult.bleDevice)
                    }
                }

    }

}

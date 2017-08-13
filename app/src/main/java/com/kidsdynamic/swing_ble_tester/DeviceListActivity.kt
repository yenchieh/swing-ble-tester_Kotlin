package com.kidsdynamic.swing_ble_tester

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity
import com.polidea.rxandroidble.RxBleClient
import com.polidea.rxandroidble.RxBleDevice
import com.polidea.rxandroidble.RxBleScanResult
import kotlinx.android.synthetic.main.activity_device_list.*
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.setContentView
import rx.Subscription


class DeviceListActivity : AppCompatActivity() {
    private lateinit var scanSubscription: Subscription
    private lateinit var adapter : DeviceListAdapter
    private val deviceList = ArrayList<RxBleDevice>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_list)
        adapter = DeviceListAdapter(deviceList)

        deviceListToolBar.setTitle(R.string.device_list_title)
        setSupportActionBar(deviceListToolBar)

        deviceListView.adapter = adapter

        deviceListView.setOnItemClickListener { adapterView, view, i, l ->
            scanSubscription.unsubscribe()
            val intent = Intent()
            println(adapter.getItem(i).macAddress)
            intent.putExtra("macAddress", adapter.getItem(i).macAddress)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
        startBle()
    }

    fun startBle() {
        val rxBleClient = RxBleClient.create(this)

        scanSubscription = rxBleClient.scanBleDevices()
                .subscribe { rxBleScanResult ->
                    if(rxBleScanResult.bleDevice?.name != null && !deviceList.contains(rxBleScanResult.bleDevice)) {
                        deviceList.add(rxBleScanResult.bleDevice)
                        adapter.add(rxBleScanResult.bleDevice)
                    }
                }
    }

}

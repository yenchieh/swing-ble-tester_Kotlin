package com.kidsdynamic.swing_ble_tester

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.polidea.rxandroidble.RxBleClient
import com.polidea.rxandroidble.RxBleDevice
import kotlinx.android.synthetic.main.activity_device_list.*
import rx.Subscription
import kotlin.concurrent.thread


class DeviceListActivity : AppCompatActivity() {
    private lateinit var scanSubscription: Subscription
    private lateinit var adapter: DeviceListAdapter
    private val deviceList = ArrayList<RxBleDevice>()
    private var scanning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_list)
        adapter = DeviceListAdapter(deviceList)
        updateStatus("Scanning...")
        setSupportActionBar(deviceListToolBar)

        deviceListView.adapter = adapter


        deviceListView.setOnItemClickListener { adapterView, view, i, l ->
            scanSubscription.unsubscribe()
            val intent = Intent()
            intent.putExtra("macAddress", adapter.getItem(i).macAddress)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
        startBle()
    }

    fun startBle() {
        if(scanning) {
            return
        }
        updateStatus("Scanning...")
        val rxBleClient = RxBleClient.create(this)
        scanning = true
        scanSubscription = rxBleClient.scanBleDevices()
                .subscribe(
                        { rxBleScanResult ->
                            if (rxBleScanResult.bleDevice?.name != null && !deviceList.contains(rxBleScanResult.bleDevice)) {
                                deviceList.add(rxBleScanResult.bleDevice)
                                adapter.add(rxBleScanResult.bleDevice)
                            }
                        },
                        { throwable ->
                            throwable.printStackTrace()
                            println(throwable.cause)
                            println("Scan device Error: ${throwable.message}")
                        }
                )


        thread {
            Thread.sleep(5000)
            scanning = false
            updateStatus("Completed")
            if(scanSubscription.isUnsubscribed){
                scanSubscription.unsubscribe()
            }

        }
    }

    override fun onStop() {
        super.onStop()
        scanning = false
        if(scanSubscription.isUnsubscribed) {
            scanSubscription.unsubscribe()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_bar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        startBle()
        return super.onOptionsItemSelected(item)
    }

    fun updateStatus(text: String) {
        runOnUiThread { deviceListToolBar.title = "" + text }
    }
}

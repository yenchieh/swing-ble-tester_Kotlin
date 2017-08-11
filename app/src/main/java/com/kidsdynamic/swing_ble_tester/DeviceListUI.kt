package com.kidsdynamic.swing_ble_tester

import android.view.View
import android.widget.ListView
import org.jetbrains.anko.*

/**
 * Created by yen-chiehchen on 8/10/17.
 */

class DeviceListUI(val deviceListAdapter: DeviceListAdapter) : AnkoComponent<DeviceListActivity> {
    override fun createView(ui: AnkoContext<DeviceListActivity>): View = with(ui) {
        return relativeLayout {
            var deviceList : ListView? =null

            //layout to display ListView
            verticalLayout {
                deviceList=listView {
                    adapter = deviceListAdapter
//                    onItemLongClick { adapterView, view, i, l ->
//                        val options = listOf("Completed","In Progress","Not Started","Delete")
//                        selector("Task Options", options) { j ->
//                            if (j == 3) {
//                                var task=adapter.getItem(i)
//                                todoAdapter?.delete(i)
//                                showHideHintListView(this@listView)
//                                longToast("Task ${task} has been deleted")
//                            }else{
//                                longToast("Task ${adapter.getItem(i).toString()} has been marked as \"${options[j]}\"")
//                            }
//                        }
//                        true
//                    }
                }
            }.lparams {
                margin = dip(5)
            }
        }
    }
}
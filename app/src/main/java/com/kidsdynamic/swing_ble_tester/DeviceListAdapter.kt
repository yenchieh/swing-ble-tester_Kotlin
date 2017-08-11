package com.kidsdynamic.swing_ble_tester

import android.graphics.Typeface
import android.graphics.Typeface.DEFAULT_BOLD
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout.HORIZONTAL
import com.polidea.rxandroidble.RxBleDevice
import org.jetbrains.anko.*
import java.util.*

class DeviceListAdapter(val list: ArrayList<RxBleDevice> = ArrayList<RxBleDevice>()) : BaseAdapter() {
    override fun getView(i : Int, v : View?, parent : ViewGroup?) : View {
        return with(parent!!.context) {
            //taskNum will serve as the S.No. of the list starting from 1
            var taskNum: Int = i +1

            //Layout for a list view item
            linearLayout {
                id = R.id.listItemContainer
                lparams(width = matchParent, height = wrapContent)
                padding = dip(10)
                orientation = HORIZONTAL

                textView {
                    id = R.id.deviceName
                    text=list.get(i).name
                    textSize = 16f
                    typeface = Typeface.MONOSPACE
                    padding =dip(5)
                }

                textView {
                    id = R.id.deviceMacId
                    text=list.get(i).macAddress
                    textSize = 16f
                    typeface = DEFAULT_BOLD
                    padding =dip(5)
                }
            }
        }
    }

    override fun getItem(position : Int) : RxBleDevice {
        return list[position]
    }

    override fun getCount() : Int {
        return list.size
    }

    override fun getItemId(position : Int) : Long {
        //can be used to return the item's ID column of table
        return 0L
    }

    //function to add an item to the list
    fun add(bleDevice: RxBleDevice) {
        list.add(list.size, bleDevice)
        notifyDataSetChanged()
    }

    //function to delete an item from list
    fun delete(i:Int) {
        list.removeAt(i)
        notifyDataSetChanged()
    }


}
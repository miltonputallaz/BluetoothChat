package com.sanicorporation.bluetoothchat.adapter

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCallback
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class DeviceAdapter(context: Context, var deviceList: ArrayList<BluetoothDevice>, val callback: (device: BluetoothDevice)-> Unit):ArrayAdapter<BluetoothDevice>(context, android.R.layout.select_dialog_item) {

    override fun getCount(): Int {
        return deviceList.size
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = LayoutInflater.from(context)
        val device = deviceList[position]
        var view = convertView
        if (view == null){
            view = inflater.inflate(android.R.layout.select_dialog_item, parent,false)
        }

        val name: TextView = view!!.findViewById<TextView>(android.R.id.text1)
        name.text = device.name ?: device.address
        view.setOnClickListener { callback(device) }
        return view
    }

    override fun add(`object`: BluetoothDevice?) {
        deviceList.add(`object`!!)
        notifyDataSetChanged()
    }
}
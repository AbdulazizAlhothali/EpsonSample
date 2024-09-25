package com.example.epsonsample

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.SimpleAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.epson.epos2.Epos2Exception
import com.epson.epos2.discovery.Discovery
import com.epson.epos2.discovery.DiscoveryListener
import com.epson.epos2.discovery.FilterOption
import com.example.epsonsample.databinding.ActivityMainBinding

class PrinterDiscoveryActivity : AppCompatActivity(), OnItemClickListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var printerListAdapter: SimpleAdapter
    private var filterOption: FilterOption? = null
    private lateinit var printerList: ArrayList<HashMap<String, String>>

    private val discoveryListener =
        DiscoveryListener { deviceInfo ->
            runOnUiThread {
                val item = java.util.HashMap<String, String>()
                item["PrinterName"] = deviceInfo.deviceName
                item["Target"] = deviceInfo.target
                printerList.add(item)
                printerListAdapter.notifyDataSetChanged()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        printerList = arrayListOf()
        printerListAdapter = SimpleAdapter(
            this,
            printerList,
            R.layout.printer_item,
            arrayOf("PrinterName", "Target"),
            intArrayOf(R.id.PrinterName, R.id.Target)
        )

        binding.lstReceiveData.adapter = printerListAdapter
        binding.lstReceiveData.onItemClickListener = this
        filterOption = FilterOption()
        filterOption?.deviceType = Discovery.TYPE_PRINTER
        filterOption?.epsonFilter = Discovery.FILTER_NAME
        requestBluetooth()


        binding.btnRestart.setOnClickListener {
            restartDiscovery()
        }

        setContentView(binding.root)

    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        //Todo: Save this in sharedPref to be used in printing
        val target = printerList[position].get("Target")

        Log.d("target", target.toString())
    }

    private fun restartDiscovery() {
        try {
            Discovery.stop()
        } catch (e: Epos2Exception) {
            if (e.errorStatus != Epos2Exception.ERR_PROCESSING) {
                e.printStackTrace()
            }
        }

        printerList.clear()
        printerListAdapter.notifyDataSetChanged()

        try {
            Discovery.start(this, filterOption, discoveryListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun requestBluetooth() {
        // check android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestMultiplePermissions.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        } else {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestEnableBluetooth.launch(enableBtIntent)
        }
    }

    private val requestEnableBluetooth =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // granted
                try {
                    Discovery.start(this, filterOption, discoveryListener)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                // denied
            }
        }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            when {
                permissions[Manifest.permission.BLUETOOTH_SCAN] == true && permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                    try {
                        Discovery.start(this, filterOption, discoveryListener)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

    override fun onDestroy() {
        super.onDestroy()
        try {
            Discovery.stop()

        } catch (e: Epos2Exception) {
            if (e.errorStatus != Epos2Exception.ERR_PROCESSING) {
                e.printStackTrace()
            }
        }
        filterOption = null
    }
}
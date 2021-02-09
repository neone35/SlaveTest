package com.arturmaslov.slavetest

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.arturmaslov.slavetest.databinding.ActivityMainBinding
import com.mypos.slavesdk.*
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog


private const val REQUEST_CODE_LOCATION_PERMISSION = 9635
private const val REQUEST_ENABLE_BT = 2609

class MainActivity :
    AppCompatActivity(),
    EasyPermissions.PermissionCallbacks {

    private var mPOSHandler: POSHandler? = null
    private val mBluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        POSHandler.setCurrency(Currency.EUR);
        POSHandler.setApplicationContext(this);
        mPOSHandler = POSHandler.getInstance();
        POSHandler.setLanguage(Language.LITHUANIAN);
        setUiVisible(false)
        setPosInfoListener()
        POSHandler.getInstance().setConnectionListener {
            runOnUiThread {
                Toast.makeText(this, "Device connected", Toast.LENGTH_SHORT).show()
            }
        }
        POSHandler.getInstance().setPOSReadyListener {
            Toast.makeText(this, "Device ready", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setPosInfoListener() {
        mPOSHandler!!.setPOSInfoListener(object : POSInfoListener {
            override fun onPOSInfoReceived(
                command: Int,
                status: Int,
                description: String,
                extra: Bundle
            ) {
                Log.e("onPOSInfo command ", command.toString())
                Log.e("onPOSInfo status ", status.toString())
                Log.e("onPOSInfo description ", description)
            }

            override fun onTransactionComplete(transactionData: TransactionData) {
                // handle purchase and refund transactions complete here
            }
        })
    }

    override fun onStart() {
        super.onStart()
        requestLocationPermission()
    }

    private fun setUiVisible(set: Boolean) {
        val visibility = if (set) View.VISIBLE else View.GONE
        binding.btnConnect.visibility = visibility
        binding.btnPrint.visibility = visibility
        binding.btnConnect.setOnClickListener {
            connectToPos()
        }
        binding.btnPrint.setOnClickListener {
            printName("ARTUR")
        }
    }

    private fun connectToPos() {
        POSHandler.setConnectionType(ConnectionType.BLUETOOTH);
        POSHandler.getInstance().connectDevice(this);
    }

    private fun printName(name: String) {
        Log.d("POS", "Received name $name for print")
        val receiptData = ReceiptData()
        receiptData.addRow(
            name,
            ReceiptData.Align.CENTER,
            ReceiptData.FontSize.SINGLE
        )
    }

    private fun checkBT() {
        if (mBluetoothAdapter.isEnabled) {
            setUiVisible(true)
        } else {
            askForBT()
        }
    }

    private fun askForBT() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
    }

    private fun requestLocationPermission() {
        val permissionsArray: Array<String>
        val hasPermissions: Boolean
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            permissionsArray = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
            hasPermissions = EasyPermissions.hasPermissions(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        } else {
            permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            hasPermissions = EasyPermissions.hasPermissions(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        if (!hasPermissions) {
            EasyPermissions.requestPermissions(
                host = this,
                rationale = "Coarse location access needed to find bluetooth devices",
                requestCode = REQUEST_CODE_LOCATION_PERMISSION,
                perms = permissionsArray
            )
        } else {
            checkBT()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // EasyPermissions handles the request result.
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        // Some permissions have been granted
        setUiVisible(true)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        // Some permissions have been denied
        Log.d("onPermissionsDenied", "$requestCode :${perms.size}")
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms.toString())) {
            SettingsDialog.Builder(this).build().show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            when (resultCode) {
                RESULT_OK -> setUiVisible(true)
                RESULT_CANCELED -> askForBT()
            }
        }
    }

}
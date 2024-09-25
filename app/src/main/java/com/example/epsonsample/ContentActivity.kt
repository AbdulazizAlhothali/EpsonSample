package com.example.epsonsample

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.epson.epos2.ConnectionListener
import com.epson.epos2.printer.Printer
import com.epson.epos2.printer.PrinterStatusInfo
import com.epson.epos2.printer.ReceiveListener
import com.epson.epos2.printer.StatusChangeListener
import com.example.epsonsample.databinding.ActivityContentBinding

class ContentActivity : AppCompatActivity(), ReceiveListener,
    ConnectionListener, StatusChangeListener {
    private lateinit var binding: ActivityContentBinding
    private var target: String? = null
    private var printer: Printer? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        target = intent.getStringExtra("target")
        target?.let {
            initializePrinterObject()

            binding.btnPrint.setOnClickListener {

                val screenshot: Bitmap =
                    loadBitmapFromView(
                        binding.PreviewContainerView,
                        binding.PreviewContainerView.width,
                        binding.PreviewContainerView.height
                    )

                printConnect(screenshot)
            }
        }

    }

    private fun initializePrinterObject(): Boolean {
        try {
            if (printer == null) {
                printer = Printer(
                    Printer.TM_P80,
                    Printer.MODEL_ANK,
                    this
                )
                printer?.setReceiveEventListener(this)
                printer?.setConnectionEventListener(this)
                printer?.setStatusChangeEventListener(this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }


        return true
    }

    override fun onPtrReceive(printer: Printer?, i: Int, printerStatusInfo: PrinterStatusInfo?, s: String?) {

        Log.v("PRINTER", printerStatusInfo.toString() + " : " + s)
    }

    override fun onConnection(p0: Any?, p1: Int) {
        Log.v("PRINTER > onConnection", "$p1")
    }

    override fun onPtrStatusChange(p0: Printer?, p1: Int) {
        Log.v("PRINTER on StatusChange", "$p1")
    }

    private fun isPrintable(status: PrinterStatusInfo?): Boolean {
        if (status == null) {
            return false
        }

        if (status.connection == Printer.FALSE) {
            return false
        } else if (status.online == Printer.FALSE) {
            return false
        }

        return true
    }

    private fun createReceiptData(bodyScreenShot: Bitmap): Boolean {

        if (printer == null) {
            return false
        }

        try {
            printer?.clearCommandBuffer()
            printer?.addTextAlign(Printer.ALIGN_CENTER)

            printer?.addImage(
                bodyScreenShot, 0, 0,
                bodyScreenShot.width,
                bodyScreenShot.height,
                Printer.COLOR_1,
                Printer.MODE_MONO,
                Printer.HALFTONE_DITHER,
                Printer.PARAM_DEFAULT.toDouble(),
                Printer.COMPRESS_AUTO
            )
            printer?.addFeedLine(2)
            printer?.addCut(Printer.CUT_FEED)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

        return true
    }

    private fun finalizeObject() {
        if (printer == null) {
            return
        }

        printer?.clearCommandBuffer()
        printer?.setReceiveEventListener(this)
    }

    private fun printData(): Boolean {
        if (printer == null) {
            return false
        }

        if (!connectPrinter()) {
            return false
        }

        val status: PrinterStatusInfo? = printer?.status


        if (!isPrintable(status)) {
            try {
                printer?.disconnect()
            } catch (ex: java.lang.Exception) {
                ex.printStackTrace()
                // Do nothing
            }
            return false
        }

        try {
            printer?.sendData(Printer.PARAM_DEFAULT)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            try {
                printer?.disconnect()
            } catch (ex: java.lang.Exception) {
                ex.printStackTrace()
                // Do nothing
            }
            return false
        }

        return true
    }

    private fun connectPrinter(): Boolean {
        if (printer == null) {
            return false
        }

        try {
            if (printer?.status?.connection == Printer.FALSE) {
                printer?.connect(target, Printer.PARAM_DEFAULT)
                try {
                    printer?.beginTransaction()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

        return true
    }


    private fun printConnect(bodyScreenShot: Bitmap?): Boolean {
        if (printer == null) {
            return false
        }

        if (!createReceiptData(bodyScreenShot!!)) {
            finalizeObject()
            return false
        }

        if (!printData()) {
            finalizeObject()
            return false
        }
        return true
    }

    private fun loadBitmapFromView(v: View, width: Int, height: Int): Bitmap {
        val b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        v.draw(c)

        return b
    }

    private fun disconnectPrinter() {
        if (printer == null) {
            return
        }

        try {
            printer?.endTransaction()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            printer?.disconnect()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        finalizeObject()
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectPrinter()
    }
}
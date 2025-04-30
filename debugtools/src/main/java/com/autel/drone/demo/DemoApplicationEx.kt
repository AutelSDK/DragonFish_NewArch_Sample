package com.autel.drone.demo

import android.app.Application
import android.net.wifi.WifiInfo
import android.util.Log
import com.autel.drone.sdk.vmodelx.SDKInitConfig
import com.autel.drone.sdk.vmodelx.SDKManager
import com.autel.drone.sdk.vmodelx.interfaces.IAutelLog
import com.autel.drone.sdk.vmodelx.manager.FrequencyBandManager
import com.autel.sdk.df.wifimanager.BetaWifiManager
import com.autel.sdk.df.wifimanager.IBetaWifiListener
import com.autel.sdk.df.wifimanager.WifiResultBean

class DemoApplicationEx : Application() {

    override fun onCreate() {
        super.onCreate()
        //SDKManager.get().init(applicationContext, true)
        
        val logImp = object : IAutelLog {
            override fun d(tag: String, msg: String, throwable: Throwable?) {
                Log.d(tag, msg)
            }

            override fun e(tag: String, msg: String, throwable: Throwable?) {
                Log.e(tag, msg)
            }

            override fun i(tag: String, msg: String, throwable: Throwable?) {
                Log.i(tag, msg)
            }

            override fun w(tag: String, msg: String, throwable: Throwable?) {
                Log.w(tag, msg)
            }
        }

        /**
         * MSDK init parameter setting
         */
        val sdkInitConfig = SDKInitConfig().apply {
            /**
             * debug mode
             */
            debug = false

            /**
             * app type: Reserved field, do not set
             */
            appType = null


            /**
             * video render thread switch
             * if nest mode, can close render thread, Improve performance
             */
            bRender = true

            /**
             * handle log by implementation of IAutelLog
             */
            log = logImp

            /**
             * true : single mode , false: mesh mode
             * can auto config when sdk initialize, if not success ,you can manually config
             */
            single = false //, //if single is null will auto check mode

            /**
             * Whether DragonFish WiFi base station is supported
             */
            bSupportBaseStation = true

            /**
             * handle data storage by implementation of IAutelStorage
             */
            storage = null
        }

        SDKManager.get().init(applicationContext, sdkInitConfig)
        initWifiBaseStation()
        println("SDKManager V=${SDKManager.get().getSDKVersion()}")
    }

    /**
     * Wifi base station connect setting
     */
    private fun initWifiBaseStation(){
        if (!supportBetaWifi()) {
            println("App init not support BetaWifiManager")
            return
        }

        BetaWifiManager.get().addListener(object : IBetaWifiListener {

            override fun wifiConnect(connect: Boolean, wifiInfo: WifiInfo?) {
                println("App init wifiConnect connect=$connect, wifiInfo=$wifiInfo")
            }

            override fun scanResults(list: List<WifiResultBean>) {
                println("App init scanResults=${list.size}")
            }

            override fun wifiRssi(rssi: Int, connectState: Int) {
                println("App init wifiRssi rssi=$rssi, connectState=$connectState")
            }
        })

        BetaWifiManager.get().setAutoConnect(true)
        BetaWifiManager.get().startScan()
        println("App init start BetaWifiManager")
    }

    private fun supportBetaWifi(): Boolean {
        return try {
            Class.forName("android.net.vci.VciManager")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
}
package com.autel.sdk.debugtools.helper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.log.SDKLog
import com.autel.drone.sdk.vmodelx.interfaces.IKeyManager
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.CommonKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.dragonfish.DFCommonCmdKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.dragonfish.WifiBaseStationKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.dragonfish.bean.DFAutoCheckResultBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.dragonfish.bean.WifiBaseStationStatusNtfyBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.dragonfish.bean.enums.AutoCheckTypeEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.bean.DroneSystemStateLFNtfyBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.AutoCheckStateEnum


class PreFlightCheckHelper(private val context: Context, private val onResult: (Int) -> Unit) {

    companion object {
        private const val TAG = "PreFlightCheckHelper"

        const val CHECK_SUCCESS = 0
        const val CHECK_FAIL = -1
    }

    private val keyLFNtfy = KeyTools.createKey(CommonKey.KeyDroneSystemStatusLFNtfy)
    private val stationStatusNtfy = KeyTools.createKey(WifiBaseStationKey.KeyBaseStationStatusNtfy)

    private var keyManager: IKeyManager? = null
    private var rcBatterRemaining = 0
    private var stationBatteryRemaining = 0

    private val checkListener = object: CommonCallbacks.KeyListener<DroneSystemStateLFNtfyBean> {
        override fun onValueChange(oldValue: DroneSystemStateLFNtfyBean?, newValue: DroneSystemStateLFNtfyBean) {
            if (AutoCheckStateEnum.AUTO_CHECK_FINISH == newValue.autoCheckState) {
                getCheckResult()
            }
        }
    }

    private val stationStatusListener = object: CommonCallbacks.KeyListener<WifiBaseStationStatusNtfyBean> {
        override fun onValueChange(oldValue: WifiBaseStationStatusNtfyBean?, newValue: WifiBaseStationStatusNtfyBean) {
            stationBatteryRemaining = newValue.batteryLevel //BaseStation battery remaining, [0,100]
        }
    }

    private val batteryReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val remaining = intent.extras?.getInt("level") // 获得当前电量百分比
            rcBatterRemaining = remaining ?: 0
        }
    }

    init {
        keyManager = DeviceManager.getDeviceManager().getFirstDroneDevice()?.getKeyManager()
        keyManager?.listen(keyLFNtfy, checkListener)
        keyManager?.listen(stationStatusNtfy, stationStatusListener)

        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(batteryReceiver, filter)
    }

    fun startCheck() {
        val checkType = AutoCheckTypeEnum.ALL
        val key = KeyTools.createKey(DFCommonCmdKey.KeyDroneStartAutoCheck)
        SDKLog.i(TAG, "autoSafeCheck start")
        keyManager?.performAction(key, checkType, object: CommonCallbacks.CompletionCallbackWithParam<Void> {
            override fun onSuccess(t: Void?) {
                SDKLog.i(TAG, "autoSafeCheck Success")
            }

            override fun onFailure(error: IAutelCode, msg: String?) {
                SDKLog.i(TAG, "autoSafeCheck onFailure code: $error s: $msg")
                onResult(CHECK_FAIL)
            }
        })
    }

    fun release() {
        keyManager?.cancelListen(keyLFNtfy, checkListener)
        keyManager?.cancelListen(stationStatusNtfy, stationStatusListener)
        context.unregisterReceiver(batteryReceiver)
    }

    private fun getDroneBatterRemain(): Int {
        val droneStateData = DeviceManager.getDeviceManager().getFirstDroneDevice()?.getDeviceStateData()
         return droneStateData?.flightControlData?.batteryRemainingPower ?: 0
    }

    private fun getCheckResult() {
        val key = KeyTools.createKey(DFCommonCmdKey.KeyDroneGetAutoCheckResult)
        keyManager?.performAction(key, null, object: CommonCallbacks.CompletionCallbackWithParam<DFAutoCheckResultBean> {
            override fun onSuccess(t: DFAutoCheckResultBean?) {
                SDKLog.d(TAG, "KeyDroneGetAutoCheckResult onSuccess: $t")
                //all the modules check ok, then check the battery remaining
                val isCheckOK = t?.isLeftSteerNormal() == true
                        && t.isRightSteerNormal()
                        && t.isBehindSteerNormal()
                        && t.isAirSpeedNormal()
                        && t.isImu1Normal()
                        && t.isImu2Normal()
                        && (t.isGPSNormal() || t.isRTKNormal())
                        && t.isMagnetometer1normal()
                        && t.isMagnetometer2normal()
                        && t.isUltrasonicNormal()
                        && t.isBarometerNormal()
                        && t.isBatteryNormal()
                        && t.isGimbalNormal()
                        && t.isRemoteControllerNormal()
                        && t.isFontMotorNormal()
                        && t.isLeftMotorNormal()
                        && t.isRightMotorNormal()
                        && t.isBehindMotorNormal()
                        && t.isPayloadNormal()
                        && t.isAttitudeNormal()
                        && t.isRTKHeadingNormal()
                        && t.isNavigationAttitudeNormal()
                        && t.isMaintenancReminder()
                        && t.isVersionMatching()
                        && t.isElectronicFenceNormal()
                        && t.isNavigationAndRTKNormal()
                        //battery remaining check
                        && rcBatterRemaining >= 15
                        && stationBatteryRemaining >= 15
                        && getDroneBatterRemain() >= 15

                onResult(if (isCheckOK) CHECK_SUCCESS else CHECK_FAIL)
            }

            override fun onFailure(error: IAutelCode, msg: String?) {
                SDKLog.d(TAG, "KeyDroneGetAutoCheckResult onFailure code: $error s: $msg")
            }
        })
    }
}
package com.autel.sdk.debugtools.activity

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.log.SDKLog
import com.autel.drone.sdk.vmodelx.SDKManager
import com.autel.drone.sdk.vmodelx.device.IAutelDroneListener
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.interfaces.IBaseDevice
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.CameraKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.camera.enums.VideoCompressStandardEnum
import com.autel.drone.sdk.vmodelx.utils.ToastUtils
import com.autel.sdk.debugtools.*
import com.autel.sdk.debugtools.fragment.ExternalMSDKInfoFragment
import com.autel.sdk.debugtools.uploadMsg.AircraftUpMsgManager
import com.autel.sdk.debugtools.uploadMsg.DebugToolUploadMsgManager

/**
 * testing tools activity class for debug tools
 * Copyright: Autel Robotics
 * @author vikas on 2022/12/23
 */

abstract class DebugToolsActivity : AppCompatActivity(), IAutelDroneListener {

    protected val msdkCommonOperateVm: MSDKCommonOperateVm by viewModels()
    protected val msdkInfoVm: MSDKInfoVm by viewModels()

    private val testToolsVM: TestToolsVM by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_testing_tools)
        SDKManager.get().init(applicationContext, true)
        SDKManager.get().getDeviceManager().addDroneListener(this)
        DeviceManager.getDeviceManager().getFirstRemoteDevice()
            ?.let { DebugToolUploadMsgManager.listenMsg(it) }
        isDebugPage = true
        supportFragmentManager.commit {
            replace(R.id.main_info_fragment_container, ExternalMSDKInfoFragment())
        }
        AutelToastUtil.autelToastLD = testToolsVM.autelToastResult
        testToolsVM.autelToastResult.observe(this) { result ->
            result?.msg?.let {
                ToastUtils.showToast(it)
            }
        }
        loadPages()
        msdkInfoVm.titleControl.observe(this) {
            if (it) {
                findViewById<FrameLayout>(R.id.main_info_fragment_container).visibility =
                    View.VISIBLE
            } else {
                findViewById<FrameLayout>(R.id.main_info_fragment_container).visibility = View.GONE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        SDKManager.get().getDeviceManager().removeDroneListener(this)
        isDebugPage = false
        AutelToastUtil.autelToastLD = null
    }

    abstract fun loadPages()

    override fun onDroneChangedListener(connected: Boolean, drone: IAutelDroneDevice) {
        SDKLog.i("Application", "onDroneChangedListener--->$connected")
        if (connected) {
            AircraftUpMsgManager.listenMsg(drone)
            drone.getKeyManager().let {
                val key = KeyTools.createKey(CameraKey.KeyCameraTransferPayLoadType)
                it.setValue(key, VideoCompressStandardEnum.H264, object: CommonCallbacks.CompletionCallback {
                    override fun onFailure(code: IAutelCode, msg: String?) {
                        SDKLog.i("Application", "KeyCameraTransferPayLoadType onFailure--->$code, $msg")
                    }

                    override fun onSuccess() {
                        SDKLog.i("Application", "KeyCameraTransferPayLoadType onSuccess")
                    }
                })
            }
        } else {
            AircraftUpMsgManager.cancelListenMsg(drone)
        }
    }

    companion object {
        var isDebugPage = false
    }
}
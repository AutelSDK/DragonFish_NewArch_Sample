package com.autel.sdk.debugtools.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.autel.drone.sdk.SDKConstants
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.log.SDKLog
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.GimbalKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.dragonfish.DFFlightControlKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.dragonfish.bean.DFTargetInfoBean
import com.autel.drone.sdk.vmodelx.module.camera.bean.LensTypeEnum
import com.autel.player.player.autelplayer.AutelPlayer
import com.autel.player.player.autelplayer.AutelPlayerView
import com.autel.sdk.debugtools.databinding.FragmentLidarRangBinding

class LidarRangingFragment : AutelFragment() {

    companion object {
        private const val TAG = "LidarRangingFragment"
        private const val MSG_UPDATE_RESULT = 0x01
    }

    private lateinit var binding: FragmentLidarRangBinding
    private var isRunning = false
    private var autelPlayer: AutelPlayer? = null

    private val lidarKey = KeyTools.createKey(GimbalKey.KeyLaserRangingSwitch)
    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_UPDATE_RESULT -> {
                    updateResult()
                }
            }
        }
    }

    private var targetBean: DFTargetInfoBean? = null
    private val keyTargetInfoNtfy = KeyTools.createKey(DFFlightControlKey.KeyTargetInfoNtfy)
    private val targetListener = object: CommonCallbacks.KeyListener<DFTargetInfoBean> {
        override fun onValueChange(oldValue: DFTargetInfoBean?, newValue: DFTargetInfoBean) {
            targetBean = newValue
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLidarRangBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initPlayer()

        binding.btnStartRecognize.setOnClickListener {
            val keyManager = DeviceManager.getDeviceManager().getFirstDroneDevice()?.getKeyManager()
            keyManager?.setValue(lidarKey, !isRunning, object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    SDKLog.i(TAG, "set lidar success ${!isRunning}")
                    isRunning = !isRunning
                    toggleLidar()
                }

                override fun onFailure(code: IAutelCode, msg: String?) {
                    SDKLog.i(TAG, "set lidar failed ${!isRunning} $code $msg")
                }
            })
        }

        //get current status
        val keyManager = DeviceManager.getDeviceManager().getFirstDroneDevice()?.getKeyManager()
        keyManager?.getValue(lidarKey,
            object : CommonCallbacks.CompletionCallbackWithParam<Boolean> {
                override fun onSuccess(t: Boolean?) {
                    isRunning = t ?: false
                    SDKLog.i(TAG, "get lidar success $isRunning")
                    toggleLidar()
                }

                override fun onFailure(error: IAutelCode, msg: String?) {
                    SDKLog.i(TAG, "get lidar failed $error $msg")
                }
            })
    }

    private fun toggleLidar() {
        activity?.runOnUiThread {
            binding.ivLidar.isVisible = isRunning
            binding.tvResult.isVisible = isRunning
        }
        if (isRunning) {
            DeviceManager.getDeviceManager().getFirstDroneDevice()?.getKeyManager()?.listen(keyTargetInfoNtfy, targetListener)
            handler.sendEmptyMessage(MSG_UPDATE_RESULT)
        } else {
            handler.removeMessages(MSG_UPDATE_RESULT)
            DeviceManager.getDeviceManager().getFirstDroneDevice()?.getKeyManager()?.cancelListen(keyTargetInfoNtfy, targetListener)
        }
    }

    private fun updateResult() {
        val builder = StringBuilder()
        if (targetBean?.distanceValid == 1) {
            builder.append("Distance: ${targetBean?.distance}m, ")
        } else {
            builder.append("Distance: N/A, ")
        }
        if (targetBean?.speedValid == 1) {
            builder.append("Speed: ${targetBean?.speed}m/s\n")
        } else {
            builder.append("Speed: N/A\n")
        }
        if (targetBean?.posValid == 1) {
            builder.append("Latitude: ${targetBean?.latitude}°, Longitude: ${targetBean?.longitude}, Altitude: ${targetBean?.altitude}")
        } else {
            builder.append("Latitude: N/A, Longitude: N/A, Altitude: N/A")
        }

        binding.tvResult.text = builder.toString()
        handler.sendEmptyMessageDelayed(MSG_UPDATE_RESULT, 1000)
    }

    private fun initPlayer() {
        val codecView = createAutelCodecView()
        binding.layoutVideo.addView(codecView)
        val drone = DeviceManager.getDeviceManager().getFirstDroneDevice()
        val lens = drone?.getAbilitySetManager()?.getCameraAbilitySetManager()?.getLensList(drone.getGimbalDeviceType()) ?: emptyList()
        val lensType = if (lens.contains(LensTypeEnum.WideAngle)) LensTypeEnum.WideAngle else LensTypeEnum.Zoom
        val channelId = getChannelId(lensType)
        autelPlayer = AutelPlayer(channelId)
        autelPlayer?.addVideoView(codecView)
    }

    private fun getChannelId(lensType: LensTypeEnum): Int {
        val drone = DeviceManager.getDeviceManager().getFirstDroneDevice()
            ?: return SDKConstants.getZoomChancelId()
        val lens = drone.getAbilitySetManager().getCameraAbilitySetManager()
            .getLensList(drone.getGimbalDeviceType()) ?: emptyList()
        return when (lensType) {
            LensTypeEnum.Zoom -> if (lens.contains(LensTypeEnum.WideAngle)) SDKConstants.getTelZoomChancelId() else SDKConstants.getZoomChancelId()
            LensTypeEnum.TeleZoom -> SDKConstants.getTelZoomChancelId()
            LensTypeEnum.WideAngle -> SDKConstants.getWideAngleChannelId()
            LensTypeEnum.Thermal, LensTypeEnum.TeleThermal -> SDKConstants.getInfraredChannelId()
            LensTypeEnum.NightVision -> SDKConstants.getNightVisionChannelId()
            else -> SDKConstants.getZoomChancelId()
        }
    }

    private fun createAutelCodecView(): AutelPlayerView? {
        val codecView = AutelPlayerView(activity)
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        codecView.layoutParams = params
        return codecView
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)

        DeviceManager.getDeviceManager().getFirstDroneDevice()?.getKeyManager()?.cancelListen(keyTargetInfoNtfy, targetListener)

        autelPlayer?.removeVideoView()
        autelPlayer?.releasePlayer()
    }
}
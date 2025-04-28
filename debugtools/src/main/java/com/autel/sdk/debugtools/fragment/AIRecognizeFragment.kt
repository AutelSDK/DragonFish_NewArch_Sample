package com.autel.sdk.debugtools.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import com.autel.drone.sdk.SDKConstants
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.log.SDKLog
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.AIServiceKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.aiservice.bean.AIDetectConfigBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.aiservice.bean.DetectTrackNotifyBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.aiservice.enums.AiDetectSceneTypeEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.aiservice.enums.DetectTargetEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.aitrack.TrackTargetRectBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.enums.AiServiceStatueEnum
import com.autel.drone.sdk.vmodelx.module.camera.bean.LensTypeEnum
import com.autel.player.player.autelplayer.AutelPlayer
import com.autel.player.player.autelplayer.AutelPlayerView
import com.autel.sdk.debugtools.R
import com.autel.sdk.debugtools.databinding.FragmentAiRecognizeBinding

class AIRecognizeFragment : AutelFragment() {

    companion object {
        private const val TAG = "AIRecognizeFragment"
    }

    private lateinit var binding: FragmentAiRecognizeBinding
    private var isRunning = false
    private var isLocking = false
    private var lensTypes: List<LensTypeEnum>? = null
    private var autelPlayer: AutelPlayer? = null
    private var codecView: AutelPlayerView? = null
    private var notifyBean: DetectTrackNotifyBean? = null

    private val trackListener = object : CommonCallbacks.KeyListener<DetectTrackNotifyBean> {
        override fun onValueChange(oldValue: DetectTrackNotifyBean?, newValue: DetectTrackNotifyBean) {
            val drone = DeviceManager.getDeviceManager().getFirstDroneDevice()
            val builder = StringBuilder()
            if (drone?.getDeviceStateData()?.flightControlData?.aiEnableFunc == AiServiceStatueEnum.AI_RECOGNITION) {
                builder.append("timestamp: ${newValue.timestamp} lensId:${newValue.lensId} objNum:${newValue.objNum}\n")
                newValue.infoList.forEach {
                    val type = DetectTargetEnum.getEnglishName(DetectTargetEnum.findEnum(it.type))
                    builder.append("Type:$type status:${it.status} objectId: ${it.objectId} position:(${it.startX}, ${it.startY}, ${it.width}, ${it.height})\n")
                }
                notifyBean = newValue
            } else {
                notifyBean = null
            }
            binding.tvResult.text = builder.toString()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAiRecognizeBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnStartRecognize.setOnClickListener {
            if (isRunning) {
                if (isLocking) {
                    binding.btnStartLock.performClick()
                }

                stopRecognize()
                binding.btnStartRecognize.text = getString(R.string.debug_recognition_start)
                isRunning = false
            } else {
                startRecognize()
                binding.btnStartRecognize.text = getString(R.string.debug_recognition_stop)
                isRunning = true
            }
        }

        binding.btnStartLock.setOnClickListener {
            if (isRunning) return@setOnClickListener
            if (isLocking) {
                stopTargetLock()
                binding.btnStartLock.text = getString(R.string.debug_target_lock)
                isLocking = false
            } else {
                startTargetLock()
                binding.btnStartLock.text = getString(R.string.debug_target_unlock)
                isLocking = true
            }
        }
        val drone = DeviceManager.getDeviceManager().getFirstDroneDevice()
        lensTypes = drone?.getCameraAbilitySetManger()?.getLensList(drone.getGimbalDeviceType())
        initLensOption()
        initPlayer()
    }

    private fun startRecognize() {
        val drone = DeviceManager.getDeviceManager().getFirstDroneDevice()
        val lensTypeName =
            binding.spinner.adapter.getItem(binding.spinner.selectedItemPosition) as String
        val lensType = lensTypes?.firstOrNull { it.value == lensTypeName } ?: LensTypeEnum.Zoom
        val droneLensId = drone?.getAbilitySetManager()?.getCameraAbilitySetManager()?.getLenId(lensType, drone.getGimbalDeviceType())
        val bean = AIDetectConfigBean().apply {
            sceneType = AiDetectSceneTypeEnum.SCENE_TYPE_UNIVERSAL
            lensId = droneLensId ?: 0
        }

        //listen the recognition result
        val listenKey = KeyTools.createKey(AIServiceKey.KeyAiDetectTarget)
        drone?.getKeyManager()?.listen(listenKey, trackListener)

        //start recognition
        drone?.getTrackMissionManager()?.enterTrackingMission(
            bean,
            object : CommonCallbacks.CompletionCallbackWithParam<Void> {
            override fun onSuccess(t: Void?) {
                SDKLog.d(TAG, "start recognize success")
            }

            override fun onFailure(error: IAutelCode, msg: String?) {
                SDKLog.d(TAG, "start recognize failed $error $msg")
            }
        })
    }

    private fun stopRecognize() {
        val drone = DeviceManager.getDeviceManager().getFirstDroneDevice() ?: return
        drone.getTrackMissionManager()
            .exitTrackingMission(object : CommonCallbacks.CompletionCallbackWithParam<Void> {
                override fun onSuccess(t: Void?) {
                    SDKLog.i(TAG, "stop recognize success")
                }

                override fun onFailure(error: IAutelCode, msg: String?) {
                    SDKLog.e(TAG, "stop recognize failed $error $msg")
                }
            })

        // Stop recognition
        val listenKey = KeyTools.createKey(AIServiceKey.KeyAiDetectTarget)
        drone.getKeyManager().cancelListen(listenKey, trackListener)
    }

    private fun startTargetLock() {
        val drone = DeviceManager.getDeviceManager().getFirstDroneDevice() ?: return
        val info = notifyBean?.infoList?.firstOrNull() ?: return
        val bean = TrackTargetRectBean(
            info.startX,
            info.startY,
            info.width,
            info.height,
            notifyBean?.lensId,
            AiDetectSceneTypeEnum.SCENE_TYPE_UNIVERSAL,
            info.objectId
        )
        drone.getTrackMissionManager()
            .selectTargetRect(bean, object : CommonCallbacks.CompletionCallbackWithParam<Void> {
                override fun onSuccess(t: Void?) {
                    SDKLog.d(TAG, "[selectTargetRect][onSuccess]")
                }

                override fun onFailure(error: IAutelCode, msg: String?) {
                    SDKLog.e(TAG, "[selectTargetRect][onFailure][error]$error")
                }
            })
    }

    private fun stopTargetLock() {
        val drone = DeviceManager.getDeviceManager().getFirstDroneDevice() ?: return
        val bean = TrackTargetRectBean(0.5f, 0.5f, 0f, 0f, 0)
        drone.getTrackMissionManager()
            .selectTargetRect(bean, object : CommonCallbacks.CompletionCallbackWithParam<Void> {
                override fun onSuccess(t: Void?) {
                    SDKLog.d(TAG, "[selectTargetRect][onSuccess]")
                }

                override fun onFailure(error: IAutelCode, msg: String?) {
                    SDKLog.e(TAG, "[selectTargetRect][onFailure][error]$error")
                }
            })
    }

    private fun initLensOption() {
        val data = lensTypes?.map { it.value } ?: emptyList()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, data)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinner.adapter = adapter
        binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (isRunning) {
                    stopRecognize()
                    startRecognize()
                }

                val name = adapter.getItem(position)
                val lensType = lensTypes?.firstOrNull { it.value == name } ?: LensTypeEnum.Zoom
                switchPlaySource(lensType)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
    }

    private fun initPlayer() {
        val lensType = lensTypes?.getOrNull(0) ?: LensTypeEnum.Zoom
        val channelId = getChannelId(lensType)
        codecView = createAutelCodecView()
        binding.videoView.addView(codecView)
        autelPlayer = AutelPlayer(channelId)
        autelPlayer?.addVideoView(codecView)
    }

    private fun getChannelId(lensType: LensTypeEnum): Int {
        return when (lensType) {
            LensTypeEnum.Zoom -> if (lensTypes?.contains(LensTypeEnum.WideAngle) == true) {
                SDKConstants.getTelZoomChancelId()
            } else SDKConstants.getZoomChancelId()

            LensTypeEnum.TeleZoom -> SDKConstants.getTelZoomChancelId()
            LensTypeEnum.WideAngle -> SDKConstants.getWideAngleChannelId()
            LensTypeEnum.Thermal, LensTypeEnum.TeleThermal -> SDKConstants.getInfraredChannelId()
            LensTypeEnum.NightVision -> SDKConstants.getNightVisionChannelId()
            else -> SDKConstants.getZoomChancelId()
        }
    }

    private fun createAutelCodecView(): AutelPlayerView {
        val codecView = AutelPlayerView(activity)
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        codecView.layoutParams = params
        return codecView
    }

    private fun switchPlaySource(lensType: LensTypeEnum) {
        //stop and release player
        autelPlayer?.releasePlayer()
        autelPlayer?.removeVideoView()

        //create new player
        autelPlayer = AutelPlayer(getChannelId(lensType))
        autelPlayer?.addVideoView(codecView)
    }
}
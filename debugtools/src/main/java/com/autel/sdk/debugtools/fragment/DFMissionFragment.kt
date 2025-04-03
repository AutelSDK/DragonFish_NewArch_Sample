package com.autel.sdk.debugtools.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.log.SDKLog
import com.autel.drone.sdk.vmodelx.interfaces.IMissionManager
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.dragonfish.bean.DFCommonAck
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.dragonfish.bean.DFStartMissionBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.mission.bean.MissionWaypointStatusReportNtfyBean
import com.autel.drone.sdk.vmodelx.module.fileservice.FileTransmitListener
import com.autel.drone.sdk.vmodelx.module.fileservice.FileTypeEnum
import com.autel.lib.jniHelper.NativeHelper
import com.autel.lib.jniHelper.PathPlanningParameter
import com.autel.sdk.debugtools.databinding.FragmentMissionDragonBinding
import com.autel.sdk.debugtools.fragment.AutelFragment
import com.autel.sdk.debugtools.helper.PreFlightCheckHelper
import com.google.gson.Gson
import java.io.File

class DFMissionFragment : AutelFragment() {

    companion object {
        private const val TAG = "DFMissionFragment"
    }

    private lateinit var binding: FragmentMissionDragonBinding

    private val logList: MutableList<String> = arrayListOf()
    private val handler = Handler(Looper.getMainLooper())
    private var currentStep = 0

    private var missionManager: IMissionManager? = null
    private var autFilePath: String? = null
    private var missionId: Int = 0

    private var checkHelper: PreFlightCheckHelper? = null
    private val onCheckResult: (Int) -> Unit = {
        if (it == PreFlightCheckHelper.CHECK_SUCCESS) {
            currentStep += 1
            enableButton(currentStep)
            updateLogInfo("Pre-flight check success")
        } else {
            updateLogInfo("Pre-flight check failed", true)
        }
    }

    private val missionListener = object : CommonCallbacks.KeyListener<MissionWaypointStatusReportNtfyBean> {
        override fun onValueChange(oldValue: MissionWaypointStatusReportNtfyBean?, newValue: MissionWaypointStatusReportNtfyBean) {
            updateLogInfo("mission[${newValue.missionId}, ${newValue.guid}] status: ${newValue.status}")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentMissionDragonBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        enableButton(currentStep)
        checkHelper = PreFlightCheckHelper(requireContext(), onCheckResult)

        missionManager = DeviceManager.getDeviceManager().getFirstDroneDevice()?.getWayPointMissionManager()
        missionManager?.addWaypointMissionExecuteStateListener(missionListener)

        binding.btnWrite.setOnClickListener {
            activity?.let { ctx ->
                autFilePath = ctx.cacheDir?.absolutePath + "/task_${System.currentTimeMillis()}.aut"
                ctx.assets?.open("task.json")?.use {
                    val parameter = Gson().fromJson(it.bufferedReader(), PathPlanningParameter::class.java)
                    if(NativeHelper.writeNewMissionFile(autFilePath, parameter) == 0) {
                        updateLogInfo("write mission file success")
                        currentStep += 1
                        enableButton(currentStep)
                    } else {
                        updateLogInfo("convert mission file failed", true)
                    }
                }
            }
        }

        binding.btnCheck.setOnClickListener {
            checkHelper?.startCheck()
        }
        binding.btnUpload.setOnClickListener {
            if (autFilePath.isNullOrEmpty()) {
                updateLogInfo("mission file is empty", true)
                return@setOnClickListener
            }
            missionId = System.currentTimeMillis().toInt()
            missionManager?.uploadMissionByPath(autFilePath!!, missionId.toLong(), object :  CommonCallbacks.CompletionCallbackWithProgressAndParam<Long> {
                override fun onProgressUpdate(progress: Double) {
                    updateLogInfo("upload mission progress: $progress")
                }

                override fun onFailure(error: IAutelCode, msg: String?) {
                    updateLogInfo("upload mission failed: $msg", true)
                }

                override fun onSuccess(t: Long?) {
                    if (t == missionId.toLong()) {
                        updateLogInfo("upload mission success")
                        currentStep += 1
                        enableButton(currentStep)
                    }
                }
            })
        }

        binding.btnStart.setOnClickListener {
            val bean = DFStartMissionBean(0, missionId)
            missionManager?.startDragonFishMission(bean, object : CommonCallbacks.CompletionCallbackWithParam<DFCommonAck> {
                override fun onSuccess(t: DFCommonAck?) {
                    updateLogInfo("start mission success")
                    currentStep += 1
                    enableButton(currentStep)
                }

                override fun onFailure(error: IAutelCode, msg: String?) {
                    updateLogInfo("start mission failed: $msg", true)
                }
            })
        }

        binding.btnExit.setOnClickListener {
            missionManager?.stopDragonFishMission(object: CommonCallbacks.CompletionCallbackWithParam<Void> {
                override fun onSuccess(t: Void?) {
                    SDKLog.d(TAG, "stopDragonFishMission onSuccess")
                    currentStep = 3
                    enableButton(currentStep)
                }

                override fun onFailure(error: IAutelCode, msg: String?) {
                    SDKLog.e(TAG, "stopDragonFishMission onFailure: $msg")
                }
            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        checkHelper?.release()
        missionManager?.removeWaypointMissionExecuteStateListener(missionListener)
    }

    private fun enableButton(step: Int) {
        when (step) {
            0 -> {
                binding.btnWrite.isEnabled = true
                binding.btnCheck.isEnabled = false
                binding.btnUpload.isEnabled = false
                binding.btnStart.isEnabled = false
                binding.btnExit.isEnabled = false
            }
            1 -> {
                binding.btnWrite.isEnabled = true
                binding.btnCheck.isEnabled = true
                binding.btnUpload.isEnabled = false
                binding.btnStart.isEnabled = false
                binding.btnExit.isEnabled = false
            }
            2 -> {
                binding.btnWrite.isEnabled = true
                binding.btnCheck.isEnabled = true
                binding.btnUpload.isEnabled = true
                binding.btnStart.isEnabled = false
                binding.btnExit.isEnabled = false
            }
            3 -> {
                binding.btnWrite.isEnabled = true
                binding.btnCheck.isEnabled = true
                binding.btnUpload.isEnabled = true
                binding.btnStart.isEnabled = true
                binding.btnExit.isEnabled = false
            }
            4 -> {
                binding.btnWrite.isEnabled = false
                binding.btnCheck.isEnabled = false
                binding.btnUpload.isEnabled = false
                binding.btnStart.isEnabled = false
                binding.btnExit.isEnabled = true
            }
        }
    }

    private fun updateLogInfo(log: String, isError: Boolean = false) {
        logList.add(log)
        if (isError) {
            SDKLog.e(TAG, log)
        }

        val stringBuffer = StringBuffer()
        logList.forEach {
            stringBuffer.append(it).append("\n")
        }
        handler.post {
            binding.tvLogInfo.text = stringBuffer.toString()
        }
    }
}
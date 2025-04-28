package com.autel.sdk.debugtools.fragment

import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.vmodelx.enums.LocationCoordinate2D
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.FlyZoneManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.module.fileservice.FileTypeEnum
import com.autel.drone.sdk.vmodelx.module.flyzone.bean.FlyZoneAuthInformation
import com.autel.drone.sdk.vmodelx.module.flyzone.bean.FlyZoneInformation
import com.autel.sdk.debugtools.KeyValueDialogUtil
import com.autel.sdk.debugtools.R
import com.autel.sdk.debugtools.UriConvert
import com.autel.sdk.debugtools.databinding.FragmentFlyZoneBinding
import com.google.gson.Gson
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

class FlyZoneFragment : AutelFragment() {

    private lateinit var binding: FragmentFlyZoneBinding
    private val logMessage = StringBuilder()
    companion object {
        const val TAG = "FlyZoneFragment"
        private const val LISTEN_RECORD_MAX_LENGTH = 6000
    }

    private val nfzAll: MutableList<FlyZoneInformation> = mutableListOf()
    private val nfzFixed: MutableList<FlyZoneInformation> = mutableListOf()
    private val nfzTemp: MutableList<FlyZoneInformation> = mutableListOf()
    private val nfzImported: MutableList<FlyZoneInformation> = mutableListOf()
    private val nfzAuth: MutableList<FlyZoneAuthInformation> = mutableListOf()
    private var filePath: String = ""

    private val launcher = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
        if (it == null || it == Uri.EMPTY) {
            binding.tvResult.text = appendLogMessageRecord("File is null")
            return@registerForActivityResult
        }

        val path = UriConvert.getPhotoPathFromContentUri(this.requireContext(), it)
        binding.tvResult.text = appendLogMessageRecord("File Path: $path")
        if (TextUtils.isEmpty(path) || File(path!!).exists().not()) return@registerForActivityResult
        FlyZoneManager.get().importFlySafeDynamicDatabaseToMSDK(path, object: CommonCallbacks.CompletionCallbackWithParam<List<FlyZoneInformation>> {
            override fun onSuccess(t: List<FlyZoneInformation>?) {
                val result = Gson().toJson(t)
                Log.d(TAG, "onSuccess: $result")
                binding.tvResult.text = appendLogMessageRecord(result)
            }

            override fun onFailure(error: IAutelCode, msg: String?) {
                Log.d(TAG, "onFailure: $error $msg")
                binding.tvResult.text = appendLogMessageRecord("onFailure: $error $msg")
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentFlyZoneBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView() {
        FlyZoneManager.get().getFlySafeDynamicDataVersion().let {
            binding.tvResult.text = appendLogMessageRecord("FlySafeDynamicDataVersion: $it")
        }

        FlyZoneManager.get().pushFlySafeDynamicDatabaseToAircraft(object: CommonCallbacks.UpLoadFileCallbackWithProgress<Int>{
            override fun onStartUploading() {
                binding.tvResult.text = appendLogMessageRecord("onStartUploading")
            }

            override fun onSuccess() {
                binding.tvResult.text = appendLogMessageRecord("onSuccess")
            }

            override fun onFailure(error: IAutelCode) {
                binding.tvResult.text = appendLogMessageRecord("onFailure $error")
            }

            override fun onProgressUpdate(progress: Int) {
                binding.tvResult.text = appendLogMessageRecord("onProgressUpdate $progress")
            }
        })

        binding.tvResult.movementMethod = ScrollingMovementMethod.getInstance()

        binding.queryNfz.setOnClickListener { _ ->
            val location = Gson().toJson(LocationCoordinate2D(22.5955975, 113.9977696))
            KeyValueDialogUtil.showInputDialog(
                requireActivity(), getString(R.string.debug_set_request_parameters), location, "", false
            ) {_, it ->
                binding.tvResult.text = appendLogMessageRecord(it)
                FlyZoneManager.get().getFlyZonesInSurroundingArea(Gson().fromJson(it, LocationCoordinate2D::class.java), object: CommonCallbacks.CompletionCallbackWithParam<List<FlyZoneInformation>?>{
                    override fun onSuccess(t: List<FlyZoneInformation>?) {
                        nfzAll.clear()
                        if (t?.isNotEmpty() == true) {
                            nfzAll.addAll(t)
                        }
                        val result = Gson().toJson(t)
                        Log.d(TAG, "getFlyZonesInSurroundingArea onSuccess: $result")
                        binding.tvResult.text = appendLogMessageRecord(result)
                    }

                    override fun onFailure(error: IAutelCode, msg: String?) {
                        Log.e(TAG, "getFlyZonesInSurroundingArea onFailure: $error $msg")
                        binding.tvResult.text = appendLogMessageRecord("onFailure: $error $msg")
                    }
                })
            }
        }

        binding.queryFixedNfz.setOnClickListener { _ ->
            val location = Gson().toJson(LocationCoordinate2D(22.5955975, 113.9977696))
            KeyValueDialogUtil.showInputDialog(
                requireActivity(), getString(R.string.debug_set_request_parameters), location, "", false
            ) {_, it ->
                binding.tvResult.text = appendLogMessageRecord(it)
                FlyZoneManager.get().getFixedNoFlyZone(Gson().fromJson(it, LocationCoordinate2D::class.java), object: CommonCallbacks.CompletionCallbackWithParam<List<FlyZoneInformation>>{
                    override fun onSuccess(t: List<FlyZoneInformation>?) {
                        nfzFixed.clear()
                        if (t?.isNotEmpty() == true) {
                            nfzFixed.addAll(t)
                        }
                        val result = Gson().toJson(t)
                        Log.d(TAG, "getFixedNoFlyZone onSuccess: $result")
                        binding.tvResult.text = appendLogMessageRecord(result)
                    }

                    override fun onFailure(error: IAutelCode, msg: String?) {
                        Log.e(TAG, "getFixedNoFlyZone onFailure: $error $msg")
                        binding.tvResult.text = appendLogMessageRecord("onFailure: $error $msg")
                    }
                })
            }
        }

        binding.queryTempNfz.setOnClickListener { _ ->
            val location = Gson().toJson(LocationCoordinate2D(22.5955975, 113.9977696))
            KeyValueDialogUtil.showInputDialog(
                requireActivity(), getString(R.string.debug_set_request_parameters), location, "", false
            ) {_, it ->
                binding.tvResult.text = appendLogMessageRecord(it)
                FlyZoneManager.get().getTempNoFlyZone(Gson().fromJson(it, LocationCoordinate2D::class.java), object: CommonCallbacks.CompletionCallbackWithParam<List<FlyZoneInformation>>{
                    override fun onSuccess(t: List<FlyZoneInformation>?) {
                        nfzTemp.clear()
                        if (t?.isNotEmpty() == true) {
                            nfzTemp.addAll(t)
                        }
                        val result = Gson().toJson(t)
                        Log.d(TAG, "getTempNoFlyZone onSuccess: $result")
                        binding.tvResult.text = appendLogMessageRecord(result)
                    }

                    override fun onFailure(error: IAutelCode, msg: String?) {
                        Log.e(TAG, "getTempNoFlyZone onFailure: $error $msg")
                        binding.tvResult.text = appendLogMessageRecord("onFailure: $error $msg")
                    }
                })
            }
        }

        binding.queryImportNfz.setOnClickListener { _ ->
            FlyZoneManager.get().getImportNoFlyZone(object: CommonCallbacks.CompletionCallbackWithParam<List<FlyZoneInformation>>{
                override fun onSuccess(t: List<FlyZoneInformation>?) {
                    nfzImported.clear()
                    if (t?.isNotEmpty() == true) {
                        nfzImported.addAll(t)
                    }
                    val result = Gson().toJson(t)
                    Log.d(TAG, "getImportNoFlyZone onSuccess: $result")
                    binding.tvResult.text = appendLogMessageRecord(result)
                }

                override fun onFailure(error: IAutelCode, msg: String?) {
                    Log.e(TAG, "getImportNoFlyZone onFailure: $error $msg")
                    binding.tvResult.text = appendLogMessageRecord("onFailure: $error $msg")
                }
            })
        }

        binding.downloadAuthZone.setOnClickListener {
            val device = DeviceManager.getDeviceManager().getFirstDroneDevice()
            if (device == null || device.getDroneSn()?.isEmpty() == true) {
                return@setOnClickListener
            }
            FlyZoneManager.get().downloadAuthFlyZoneFromServer(
                device.getDroneSn()!!,
                LocationCoordinate2D(0.0, 0.0),
                object: CommonCallbacks.CompletionCallbackWithParam<List<FlyZoneAuthInformation>?>{
                override fun onSuccess(t: List<FlyZoneAuthInformation>?) {
                    nfzAuth.clear()
                    if (t?.isNotEmpty() == true) {
                        nfzAuth.addAll(t)
                    }
                    val result = Gson().toJson(t)
                    Log.d(TAG, "downloadAuthFlyZoneFromServer onSuccess: $result")
                    binding.tvResult.text = appendLogMessageRecord(result)
                }

                override fun onFailure(error: IAutelCode, msg: String?) {
                    Log.e(TAG, "downloadAuthFlyZoneFromServer onFailure: $error $msg")
                    binding.tvResult.text = appendLogMessageRecord("onFailure: $error $msg")
                }
            })
        }

        binding.getAuthZone.setOnClickListener {
            val device = DeviceManager.getDeviceManager().getFirstDroneDevice()
            if (device == null || device.getDroneSn()?.isEmpty() == true) {
                return@setOnClickListener
            }
            FlyZoneManager.get().getAuthFlyZone(device.getDroneSn()!!, object: CommonCallbacks.CompletionCallbackWithParam<List<FlyZoneAuthInformation>?>{
                override fun onSuccess(t: List<FlyZoneAuthInformation>?) {
                    nfzAuth.clear()
                    if (t?.isNotEmpty() == true) {
                        nfzAuth.addAll(t)
                    }
                    val result = Gson().toJson(t)
                    Log.d(TAG, "getAuthFlyZone onSuccess: $result")
                    binding.tvResult.text = appendLogMessageRecord(result)
                }

                override fun onFailure(error: IAutelCode, msg: String?) {
                    Log.e(TAG, "getAuthFlyZone onFailure: $error $msg")
                    binding.tvResult.text = appendLogMessageRecord("onFailure: $error $msg")
                }
            })
        }

        binding.importGeoFile.setOnClickListener {
            launcher.launch(arrayOf("*/*"))
        }

        binding.writeNfzFile.setOnClickListener {
            val device = DeviceManager.getDeviceManager().getFirstDroneDevice()
            if (device == null || device.getDroneSn()?.isEmpty() == true) {
                return@setOnClickListener
            }
            filePath = context?.cacheDir?.absolutePath + File.separator + System.currentTimeMillis() + ".aut"
            if (nfzAll.isEmpty()) {
                nfzAll.addAll(nfzFixed)
                nfzAll.addAll(nfzTemp)
                nfzAll.addAll(nfzImported)
            }
            FlyZoneManager.get().writeNoFlyZoneFile(filePath, FileTypeEnum.ELECTRIC_BARRIER, nfzAll, nfzAuth, object: CommonCallbacks.CompletionCallback{
                override fun onSuccess() {
                    Log.d(TAG, "writeNoFlyZoneFile onSuccess")
                    binding.tvResult.text = appendLogMessageRecord("onSuccess")
                }

                override fun onFailure(code: IAutelCode, msg: String?) {
                    Log.e(TAG, "writeNoFlyZoneFile onFailure: $code $msg")
                    binding.tvResult.text = appendLogMessageRecord("onFailure $code $msg")
                }
            })
        }

        binding.uploadDrone.setOnClickListener {
            val device = DeviceManager.getDeviceManager().getFirstDroneDevice()
            if (device == null || device.getDroneSn()?.isEmpty() == true) {
                return@setOnClickListener
            }
            FlyZoneManager.get().uploadNoFlyZoneToAircraft(
                filePath,
                device.getDroneSn().orEmpty(),
                FileTypeEnum.ELECTRIC_BARRIER,
                object: CommonCallbacks.UpLoadFileCallbackWithProgress<Int> {
                    override fun onFailure(error: IAutelCode) {
                        Log.e(TAG, "uploadNoFlyZoneToAircraft onFailure: $error")
                        binding.tvResult.text = appendLogMessageRecord("onFailure: $error")
                    }

                    override fun onStartUploading() {
                        binding.tvResult.text = appendLogMessageRecord("onStartUploading")
                    }

                    override fun onSuccess() {
                        Log.d(TAG, "uploadNoFlyZoneToAircraft onSuccess")
                        binding.tvResult.text = appendLogMessageRecord("onSuccess")
                    }

                    override fun onProgressUpdate(progress: Int) {
                        binding.tvResult.text = appendLogMessageRecord("onProgressUpdate $progress")
                    }
                }
            )
        }

        binding.clearLog.setOnClickListener {
            logMessage.clear()
            binding.tvResult.text = ""
        }
    }

    private fun appendLogMessageRecord(appendStr: String?): String {
        val curTime = SimpleDateFormat("HH:mm:ss").format(Date())
        logMessage.append(curTime)
            .append(":")
            .append(appendStr)
            .append("\n")

        //长度限制
        var result = logMessage.toString()
        if (result.length > LISTEN_RECORD_MAX_LENGTH) {
            result = result.substring(result.length - LISTEN_RECORD_MAX_LENGTH)
        }
        return result
    }
}
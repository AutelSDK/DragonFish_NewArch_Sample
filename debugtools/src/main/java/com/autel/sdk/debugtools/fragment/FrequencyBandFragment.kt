package com.autel.sdk.debugtools.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import com.autel.drone.sdk.vmodelx.enums.FrequencyBand
import com.autel.drone.sdk.vmodelx.interfaces.IFrequencyBandManager
import com.autel.drone.sdk.vmodelx.manager.FrequencyBandManager
import com.autel.drone.sdk.vmodelx.manager.areacode.AreaCodeManager
import com.autel.drone.sdk.vmodelx.manager.frequency.OnFrequencyBandListener
import com.autel.drone.sdk.vmodelx.utils.ToastUtils
import com.autel.sdk.debugtools.KeyValueDialogUtil
import com.autel.sdk.debugtools.R
import com.autel.sdk.debugtools.databinding.FragmentFrequencyBandBinding

/**
 * Copyright: Autel Robotics
 * @author R24033 on 2024/9/11
 * 合规频段
 */
class FrequencyBandFragment : AutelFragment(), OnClickListener {

    private lateinit var binding: FragmentFrequencyBandBinding

    private var frequencyBandManager: IFrequencyBandManager? = null
    private val logList: MutableList<String> = arrayListOf()
    private var currentFrequencyBand: FrequencyBand = FrequencyBand.MODE_UNKNOWN

    private var supportList: List<FrequencyBand>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        frequencyBandManager = FrequencyBandManager.get()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFrequencyBandBinding.inflate(layoutInflater, container, false)
        initView()
        return binding.root
    }

    private fun initView() {
        binding.tvCountryCode.text =
            getString(R.string.debug_country_code, AreaCodeManager.get().getAreaCode()?.areaCode)

//        getSupportFrequencyList()
        binding.btnRegisterListener.setOnClickListener(this)
        binding.btnUnregisterListener.setOnClickListener(this)
        binding.btnGetCurrFrequency.setOnClickListener(this)
        binding.btnSetCurrFrequency.setOnClickListener(this)
        binding.btnResetCountrycode.setOnClickListener(this)
        binding.btnClearLog.setOnClickListener(this)
    }

    private val listener: OnFrequencyBandListener = object : OnFrequencyBandListener {
        override fun onChange(country: String?, list: List<FrequencyBand>) {
            updateLogInfo(">>OnFrequencyBandListener#onChange()->country:$country;list:$list")
            binding.tvCountryCode.text =
                getString(R.string.debug_country_code, country)
            supportList = list
            getSupprtFrequencyString(list)
        }

    }

    /**
     * 主动获取飞机支持的合规频段列表
     */
    private fun getSupportFrequencyList() {
        frequencyBandManager?.getSupportFrequencyBand { list ->
            getSupprtFrequencyString(list)
        }
    }

    private fun getSupprtFrequencyString(list: List<FrequencyBand>) {
        if (list.isNullOrEmpty()) {
            updateLogInfo("[getSupprtFrequencyString]:list is null or empty")
            return
        }
        val sb = StringBuilder()
        list.forEach {
            sb.append(it.tag)
                .append("_")
        }
        val result = sb.toString()
        binding.tvSupportFrequencyList.text =
            getString(R.string.debug_support_frequency_list, result.substring(0, result.length - 1))
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_register_listener -> {
                frequencyBandManager?.addListener(listener)
                updateLogInfo(">>addListener click")
            }

            R.id.btn_unregister_listener -> {
                frequencyBandManager?.removeListener(listener)
                updateLogInfo(">>removeListener click")
            }

            R.id.btn_get_curr_frequency -> {
                updateLogInfo(">>getCurrentFrequency")
                getCurrFrequency()
            }

            R.id.btn_set_curr_frequency -> {
                updateLogInfo(">>setCurrentFrequency")
                setFrequencyBand()
            }

            R.id.btn_reset_countrycode -> {
                updateLogInfo(">>resetCountryCode")
                resetCountryCode()
            }

            R.id.btn_clear_log -> {
                clearLogList()
            }
        }
    }

    /**
     * 获取当前频段
     */
    private fun getCurrFrequency() {
        frequencyBandManager?.getCurrFrequencyBand { band ->
            currentFrequencyBand = band
            binding.tvCurrentFrequencyBand.text =
                getString(R.string.debug_current_frequency_band, band.tag)
            updateLogInfo(">>getCurrFrequency()->band:${band.tag}")
        }
    }

    /**
     * 设置频段
     */
    private fun setFrequencyBand() {
        var band = currentFrequencyBand
        KeyValueDialogUtil.showSingleChoiceDialog(
            requireContext(),
            supportList?.map { it.tag } ?: emptyList(),
            supportList?.indexOf(band).takeIf { it != -1 } ?: 0
        ) { _, t ->
            band = FrequencyBand.find(t?.firstOrNull() ?: "UNKNOWN")
            if (band == FrequencyBand.MODE_UNKNOWN) {
                ToastUtils.showToast("不支持设置MODE_UNKNOWN,请先获取一下")
                updateLogInfo(">>setFrequencyBand()->不支持设置MODE_UNKNOWN,请先获取一下")
                return@showSingleChoiceDialog
            }

            updateLogInfo(">>setFrequencyBand()->currentFrequencyBand:$band")
            frequencyBandManager?.setFrequencyBand(band, onSuccess = {
                updateLogInfo(">>setFrequencyBand()->success")
                binding.tvCurrentFrequencyBand.text =
                    getString(R.string.debug_current_frequency_band, band.tag)
            }, onFailed = { msg ->
                updateLogInfo(">>setFrequencyBand()->failed,msg:$msg")
            })
        }
    }

    /**
     * 更新日志信息
     */
    private fun updateLogInfo(msg: String) {
        logList.add(msg)
        val sb = StringBuffer()
        logList.forEach {
            sb.append(it).append("\n")
        }
        binding.tvResult.text = sb.toString()

    }

    private fun resetCountryCode() {
//        updateLogInfo(">>resetCountryCode:US")
//        SDKConfigManager.setCountryCode("US")
//        SDKConfigManager.setFrequencyPublic(flag = false)
    }

    private fun clearLogList() {
        logList.clear()
        binding.tvResult.text = ""
    }

    override fun onDestroy() {
        super.onDestroy()
        frequencyBandManager?.destory()
    }
}
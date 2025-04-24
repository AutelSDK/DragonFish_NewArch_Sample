package com.autel.sdk.debugtools.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.net.wifi.WifiInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autel.drone.sdk.log.SDKLog
import com.autel.sdk.debugtools.R
import com.autel.sdk.debugtools.databinding.FragmentWifiManagerBinding
import com.autel.sdk.df.wifimanager.BetaWifiManager
import com.autel.sdk.df.wifimanager.IBetaWifiListener
import com.autel.sdk.df.wifimanager.WifiResultBean


/**
 * wifi base station management
 * Copyright: Autel Robotics
 */
@SuppressLint("SetTextI18n")
class WifiManagerFragment : AutelFragment(), IBetaWifiListener {

    companion object {
        private const val TAG = "WifiManagerFragment"
    }

    private lateinit var binding: FragmentWifiManagerBinding

    private var adapter: WifiListAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWifiManagerBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun scanResults(list: List<WifiResultBean>) {
        if(adapter == null){
            adapter = WifiListAdapter(list)
            binding.listView.adapter = adapter
            binding.listView.setLayoutManager(LinearLayoutManager(requireContext()))
            adapter?.setOnItemClickListener{ wifiResultBean ->
                SDKLog.i(TAG, "testDetails: $wifiResultBean")
                if(wifiResultBean.isConnect){
                    showInputConfirmDialog(requireContext(), "Disconnect Tips", "start to disconnect to wifi base station", {
                        BetaWifiManager.get().disconnectWifi()
                    })
                } else {
                    showInputDialog(requireContext(), "Connect Tips", "input password to connect to wifi base station", {
                        BetaWifiManager.get().connectWifi(wifiResultBean.result, it)
                    })
                }
            }
            SDKLog.i(TAG, "scanResults11: ${list.size}")
        } else {
            adapter?.updateData(list)
            SDKLog.i(TAG, "scanResults22: ${list.size}")
        }
    }

    private fun showInputDialog(context: Context, title: String, hint: String, onConfirm: (String) -> Unit) {
        val editText = EditText(context).apply {
            this.hint = hint
            this.setPadding(50, 30, 50, 30)
        }
        val dialog = AlertDialog.Builder(context)
            .setTitle(title)
            .setView(editText)
            .setPositiveButton("确认") { _, _ ->
                val inputText = editText.text.toString().trim()
                if (inputText.isNotEmpty()) {
                    onConfirm(inputText)
                }
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }.create()
        dialog.show()
    }

    private fun showInputConfirmDialog(context: Context, title: String, hint: String, onConfirm: () -> Unit) {
        val dialog = AlertDialog.Builder(context)
            .setTitle(title) // 设置对话框标题
            .setPositiveButton("确认") { _, _ ->
                onConfirm()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }.create()
        dialog.show()
    }

    override fun wifiConnect(connect: Boolean, wifiInfo: WifiInfo?) {
        SDKLog.i(TAG, "wifiConnect: $connect, $wifiInfo")
    }

    override fun wifiRssi(rssi: Int, connectState: Int) {
        SDKLog.i(TAG, "wifiRssi: $rssi, $connectState")
    }

    private fun init() {
        BetaWifiManager.get().addListener(this)
        BetaWifiManager.get().setAutoConnect(true)
        BetaWifiManager.get().startScan()
        SDKLog.i(TAG, "init addListener")
    }

    override fun onDestroy() {
        super.onDestroy()
        BetaWifiManager.get().removeListener(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        init()
    }

    private fun initView() {
        binding.tvStartScan.setOnClickListener {
            BetaWifiManager.get().startScan()
        }

        binding.tvGetResult.setOnClickListener {
            val r = BetaWifiManager.get().getConnectWifi()
            SDKLog.i(TAG, "getConnectWifi: $r")
            val level = BetaWifiManager.get().getWiRssiLevel()
            SDKLog.i(TAG, "getWiRssiLevel: $level")
            val isConnect = BetaWifiManager.get().isConnect()
            SDKLog.i(TAG, "isConnect: $isConnect")
        }
    }


    @SuppressLint("DefaultLocale", "SetTextI18n")
    class WifiListAdapter(list: List<WifiResultBean>) : RecyclerView.Adapter<WifiListAdapter.ViewHolder>() {

        private var wifiList = list

        private var callback: ((WifiResultBean) -> Unit)? = null

        fun updateData(wifiList: List<WifiResultBean>) {
            this.wifiList = wifiList
            notifyDataSetChanged()
        }

        fun setOnItemClickListener(callback:(WifiResultBean) ->Unit) {
            this.callback = callback
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view: View = LayoutInflater.from(parent.context).inflate(R.layout.wifi_list_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val mediaBean = wifiList[position]

            SDKLog.i(TAG, "onBindViewHolder: $mediaBean")
            holder.name.text = mediaBean.result.SSID
            holder.url.text = mediaBean.result.level.toString()
            holder.url1.text = if (mediaBean.isConnect) "Connected" else "Disconnected"
            holder.container.setOnClickListener {
                callback?.invoke(mediaBean)
            }
        }

        override fun getItemCount(): Int {
            return wifiList.size
        }

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var imageView: ImageView = itemView.findViewById(R.id.imageView)
            var container:LinearLayout = itemView.findViewById(R.id.container)
            var name: TextView = itemView.findViewById(R.id.textViewName)
            var url: TextView = itemView.findViewById(R.id.textViewAddress)
            var url1: TextView = itemView.findViewById(R.id.textViewAddress1)
        }
    }
}
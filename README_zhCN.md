# MSDK V2.6版本

[English Version](README.md)

# 概述

本项目主要是展示如何使用道通公司提供的SDK V2.6来开发Android App，来控制道通龙鱼系列无人机。MSDK V2.6拥有可以控制龙鱼系列无人机硬件控制接口和软件服务接口，开放全开源的生产代码级Sample和丰富的教程，为开发者提供了具有竞争力的无人机移动端解决方案，极大的提升开发体验和效率。

当前版本支持[龙鱼全系列无人机](https://www.autelrobotics.cn/productdetail/dragonfish-series-drones/)， 包括：

* 龙鱼标准版(7kg)

* 龙鱼Pro版(15kg)

## 依赖

除MSDK外，还需要添加一下依赖
```js
//okhttp和retrofit
implementation 'com.squareup.okhttp3:logging-interceptor:4.10.0'
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.squareup.retrofit2:adapter-rxjava3:2.9.0'
implementation 'com.squareup.retrofit2:adapter-rxjava3:2.9.0'
implementation 'com.squareup.retrofit2:converter-moshi:2.9.0'
implementation 'com.squareup.okhttp3:okhttp-dnsoverhttps:4.9.3'
implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
//event-bus
implementation 'io.github.jeremyliao:live-event-bus-x:1.8.0'
implementation 'com.google.code.gson:gson:2.8.8"
implementation 'com.squareup.okhttp3:okhttp:4.10.0'
//room数据库
implementation 'androidx.room:room-ktx:2.4.2'

implementation 'com.tencent:mmkv-static:1.2.8'
```

## 功能列表

MSDK支持包括但不限于一下功能：

| 功能列表                 | 功能描述                                                     |
| ------------------------ | ------------------------------------------------------------ |
| SDKManager               | MSDK 入口管理类，用于MSDK初始化，设备连接和设备管理获取等功能 |
| IKeyManager              | Autel Key管理类，提供Autel各设备软件模块的读写访问和控制能力 |
| AutelPlayer              | 提供码流播放的类                                             |
| IMissionManager          | 智能任务管理类，提供航线任务飞行管理功能                     |
| IDeviceManager           | 设备管理、负责多设备连接，设备能力集提供                     |
| IAlbumManager            | 从无人机中获取媒体数据，以及对相册中的媒体数据进行下载、删除等功能 |
| ICameraAbilitySetManager | 相机能力集管理类，提供相机控制相关的接口                     |
| IAutelDroneDevice        | 飞机设备接口类，通过该类可以获取飞机的状态信息以及相册管理，航线管理等接口 |
| IOTAUpgradeManager       | OTA管理类，提供检查版本，升级遥控器和飞机等功能              |
| IFrequencyBandManager    | 频段管理类，提供获取支持的通信频段，切换频段，获取频段信号强度等接口 |
| IFlyZoneManager          | 飞行区域管理类，提供获取禁飞区，鉴权区接口已经解禁禁飞区相关接口 |


更多文档和教程，请查阅[开发者网站](https://developer.autelrobotics.cn/doc/v2.6/mobile_sdk/cn/00/1)
## MSDK V2.6

[中文版本](README_zhCN.md)

### Overview

This project demonstrates how to develop Android Apps using Autel's SDK V2.6 to control Autel DragonFish series drones. MSDK V2.6 provides hardware control interfaces and software service interfaces for DragonFish series drones, offering fully open-source production-level sample code and comprehensive tutorials. This enables developers to create competitive drone mobile solutions, greatly improving development experience and efficiency.

The current version supports [all DragonFish series drones](https://www.autelrobotics.cn/productdetail/dragonfish-series-drones/), product and firmware version are listed as follows:

#### Supported Product List and Firmware Version
<table>
    <tr>
        <td><strong>Product</strong></td>
        <td><strong>Firmware Version</strong></td>
    </tr>
    <tr>
        <td rowspan="4">DragonFish Standard (7kg)</td>
        <td>UAV: V12.0.1.115</td>
    </tr>
    <tr>
        <td>RC: V1.0.1.115</td>
    </tr>
    <tr>
        <td>WiFi Station: V11.0.1.115</td>
    </tr>
    <tr>
        <td>TraceAnt: V1.0.1.115</td>
    </tr>
    <tr>
        <td rowspan="4">DragonFish Pro (15kg)</td>
        <td>UAV: V13.0.0.115</td>
    </tr>
    <tr>
        <td>RC: V1.0.1.115</td>
    </tr>
    <tr>
        <td>WiFi Station: V11.0.1.115</td>
    </tr>
    <tr>
        <td>TraceAnt: V1.0.1.115</td>
    </tr>
</table>

### Dependencies

Besides MSDK, the following dependencies are required:

```groovy
//okhttp and retrofit
implementation 'com.squareup.okhttp3:okhttp:4.10.0'
implementation 'com.squareup.okhttp3:logging-interceptor:4.10.0'
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.squareup.retrofit2:adapter-rxjava3:2.9.0'
implementation 'com.squareup.retrofit2:converter-moshi:2.9.0'
implementation 'com.squareup.okhttp3:okhttp-dnsoverhttps:4.9.3'
implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
implementation 'com.google.code.gson:gson:2.8.8'
//protobuf
implementation 'com.google.protobuf:protobuf-javalite:3.21.12'
//room database
implementation 'androidx.room:room-ktx:2.4.2'
//mmkv
implementation 'com.tencent:mmkv-static:1.2.8'
```

### Feature List

MSDK supports but is not limited to the following features:

| Feature List | Description |
|--------------|-------------|
| SDKManager | MSDK entry management class, used for MSDK initialization, device connection, and device management |
| IKeyManager | Autel Key management class, providing read/write access and control capabilities for Autel device software modules |
| AutelPlayer | Class providing stream playback functionality |
| IMissionManager | Intelligent mission management class, providing flight mission management features |
| IDeviceManager | Device management, responsible for multi-device connection and device capability set provision |
| IAlbumManager | Retrieves media data from drones and provides download, deletion, and other functions for media in the album |
| ICameraAbilitySetManager | Camera capability set management class, providing camera control-related interfaces |
| IAutelDroneDevice | Drone device interface class, providing access to drone status information, album management, mission management, and other interfaces |
| IOTAUpgradeManager | OTA management class, providing version checking and upgrade functions for remote controllers and drones |
| IFrequencyBandManager | Frequency band management class, providing interfaces for supported communication bands, band switching, and signal strength monitoring |
| IFlyZoneManager | Flight zone management class, providing interfaces for restricted zones, authorization zones, and restricted zone unlocking |

For more documentation and tutorials, please visit the [Developer Website](https://developer.autelrobotics.com/doc/v2.6/mobile_sdk/en/00/1)
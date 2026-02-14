package com.clean.ipcloud


data class IpScanResult(
    val ip: String,
    val port: Int,
    val latency: Long,
    val isSuccess: Boolean,
    var colo: String = "جستجو...",
    var countryCode: String = "??",
    var exchangeStatus: String = "در حال بررسی...",
    var mtu: Int = 1420,
    var packetLoss: Int = 0
)
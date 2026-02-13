package com.clean.ipcloud

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.*
import kotlin.random.Random

class ScannerViewModel : ViewModel() {
    val foundIps = mutableStateListOf<IpScanResult>()
    val isScanning = mutableStateOf(false)
    val ipRanges = NetworkData.IP_RANGES

    val selectedIpForConverter = mutableStateOf("")

    fun startScan(selectedRanges: List<String>, threads: Int, timeout: Int, maxResults: Int, userPort: Int) {
        viewModelScope.launch(Dispatchers.Default) {
            isScanning.value = true
            foundIps.clear()
            val semaphore = Semaphore(threads)

            // حلقه اصلی اسکن
            while (isScanning.value && foundIps.size < maxResults) {
                if (!isScanning.value) break

                launch {
                    semaphore.withPermit {
                        if (!isScanning.value) return@launch

                        // ۱. انتخاب رنج و تولید آی‌پی هوشمند
                        val range = if (selectedRanges.isEmpty()) ipRanges.random() else selectedRanges.random()
                        val ip = generateSmartIp(range)
                        val port = userPort

                        // ۲. تست اولیه (Ping & Packet Loss)
                        val res = checkSocket(ip, port, timeout)

                        if (res.isSuccess) {
                            // اضافه کردن اولیه به لیست در ترد اصلی
                            withContext(Dispatchers.Main) {
                                if (foundIps.size < maxResults && foundIps.none { it.ip == ip }) {
                                    foundIps.add(res)
                                    // مرتب‌سازی اولیه بر اساس پکت لاست و پینگ
                                    foundIps.sortWith(
                                        compareBy<IpScanResult> { it.packetLoss }
                                            .thenBy { it.latency }
                                    )
                                }
                            }

                            // ۳. بررسی اطلاعات لوکیشن و وضعیت تبادل داده (Data Exchange)
                            val info = NetworkUtils.fetchCloudflareInfo(ip)
                            val status = NetworkUtils.checkDataExchange(ip, port)

                            // ۴. آپدیت نهایی آیتم و مرتب‌سازی فوق هوشمند
                            withContext(Dispatchers.Main) {
                                val index = foundIps.indexOfFirst { it.ip == ip }
                                if (index != -1) {
                                    foundIps[index] = foundIps[index].copy(
                                        colo = info.first,
                                        countryCode = info.second,
                                        exchangeStatus = status,
                                        // اگر تبادل ناموفق بود، پکت لاست را ۱۰۰ فرض کن تا برود ته لیست
                                        packetLoss = if (status != "تبادل موفق") 100 else foundIps[index].packetLoss
                                    )

                                    // مرتب‌سازی نهایی:
                                    // اولویت ۱: تبادل موفق باشد (نزولی - Trueها بالا)
                                    // اولویت ۲: کمترین پکت لاست (صعودی)
                                    // اولویت ۳: کمترین پینگ (صعودی)
                                    foundIps.sortWith(
                                        compareByDescending<IpScanResult> { it.exchangeStatus == "تبادل موفق" }
                                            .thenBy { it.packetLoss }
                                            .thenBy { it.latency }
                                    )
                                }
                            }
                        }
                    }
                }
                delay(15) // وقفه کوتاه برای مدیریت پردازش
            }
            isScanning.value = false
        }
    }

    // متد بهبود یافته برای تولید آی‌پی‌های متنوع‌تر در رنج
    private fun generateSmartIp(range: String): String {
        val parts = range.split("/")[0].split(".")
        val mask = try { range.split("/")[1].toInt() } catch (e: Exception) { 24 }

        return when {
            mask <= 16 -> {
                // برای رنج‌های بزرگ مثل 172.64.0.0/16
                "${parts[0]}.${parts[1]}.${Random.nextInt(0, 255)}.${Random.nextInt(1, 254)}"
            }
            else -> {
                // برای رنج‌های معمولی /24
                "${parts[0]}.${parts[1]}.${parts[2]}.${Random.nextInt(1, 254)}"
            }
        }
    }

    private suspend fun checkSocket(ip: String, port: Int, timeout: Int) = withContext(Dispatchers.IO) {
        var successfulAttempts = 0
        val totalAttempts = 5
        var totalLatency = 0L

        for (i in 1..totalAttempts) {
            val start = System.currentTimeMillis()
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(ip, port), timeout)
                socket.close()
                successfulAttempts++
                totalLatency += (System.currentTimeMillis() - start)
            } catch (e: Exception) {
                // این تلاش شکست خورد
            }
            delay(20) // وقفه کوتاه بین هر پکت
        }

        if (successfulAttempts > 0) {
            val avgLatency = totalLatency / successfulAttempts
            val lossPercent = ((totalAttempts - successfulAttempts) * 100) / totalAttempts
            IpScanResult(
                ip = ip,
                port = port,
                latency = avgLatency,
                isSuccess = true,
                packetLoss = lossPercent
            )
        } else {
            IpScanResult(ip, port, -1, isSuccess = false, packetLoss = 100)
        }
    }
}
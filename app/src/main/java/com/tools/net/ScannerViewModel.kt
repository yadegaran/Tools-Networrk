package com.tools.net

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class ScannerViewModel : ViewModel() {
    val foundIps = mutableStateListOf<IpScanResult>()
    var isScanning = mutableStateOf(false)
    val ipRanges = NetworkData.IP_RANGES
    var isScanningg by mutableStateOf(false)
    val selectedIpForConverter = mutableStateOf("")

    var isSpeedTesting by mutableStateOf(false)
    var speedTestProgress by mutableStateOf("")

    val scanResults = mutableStateListOf<FragmentResult>()

    var currentProgress by mutableStateOf(0f)
    var currentTestInfo by mutableStateOf("آماده برای اسکن")

    var analysisResults = mutableStateListOf<AnalysisStep>()
    var isAnalyzing by mutableStateOf(false)

    /**
     * اسکن دو فاز (مشابه ایده‌ی پروژه‌های اسکنر کلودفلر):
     * فاز ۱: پروب سریع TCP روی IPهای تصادفی رنج‌های انتخابی.
     * فاز ۲: تایید واقعی با هندشیک HTTP/Host-Spoof (تبادل داده واقعی + موقعیت).
     * علاوه‌براین، به محض پیدا شدن یک IP سالم، چند «همسایه» آن (همان بلوک /24) هم
     * صف می‌شوند چون کلودفلر معمولاً کیفیت مشابهی در یک بلوک ارائه می‌دهد.
     * هم‌زمان یک صف پس‌زمینه با همروندی محدود، سرعت دانلود/آپلود واقعی همه‌ی
     * IPهای تایید‌شده را اندازه می‌گیرد تا فشار شبکه روی فاز پروبینگ نیفتد.
     */
    fun startScan(
        selectedRanges: List<String>,
        threads: Int,
        timeout: Int,
        maxResults: Int,
        userPort: Int
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            isScanning.value = true
            isSpeedTesting = true
            foundIps.clear()
            val semaphore = Semaphore(threads)
            val triedIps = ConcurrentHashMap.newKeySet<String>()
            val speedQueue = Channel<String>(Channel.UNLIMITED)

            // ۲ کارگر پس‌زمینه برای تست سرعت واقعی، مستقل از فاز پروبینگ
            val speedWorkers = List(2) {
                launch(Dispatchers.IO) {
                    for (ip in speedQueue) {
                        withContext(Dispatchers.Main) { speedTestProgress = "تست سرعت: $ip" }
                        val download = NetworkUtils.measureDownloadMbps(ip)
                        val upload = NetworkUtils.measureUploadMbps(ip)
                        withContext(Dispatchers.Main) {
                            val index = foundIps.indexOfFirst { it.ip == ip }
                            if (index != -1) {
                                foundIps[index] = foundIps[index].copy(
                                    downloadMbps = download,
                                    uploadMbps = upload,
                                    isSpeedTested = true
                                )
                                resortFoundIps()
                            }
                        }
                    }
                }
            }

            // coroutineScope تضمین می‌کند همه‌ی پروب‌های اصلی و همسایه (حتی آن‌هایی که با تاخیر
            // لانچ شده‌اند) قبل از بستن صف تست سرعت، کامل تمام شوند - وگرنه چند IP آخر ممکن
            // بود بی‌صدا از تست سرعت جا بمانند.
            coroutineScope {
                suspend fun probeIp(ip: String, port: Int, isNeighbor: Boolean) {
                    if (!triedIps.add(ip)) return // قبلاً تست شده
                    if (!isScanning.value) return

                    val res = checkSocket(ip, port, timeout)
                    if (!res.isSuccess) return

                    withContext(Dispatchers.Main) {
                        if (foundIps.size < maxResults && foundIps.none { it.ip == ip }) {
                            foundIps.add(res)
                            foundIps.sortWith(compareBy<IpScanResult> { it.packetLoss }.thenBy { it.latency })
                        }
                    }

                    val info = NetworkUtils.fetchCloudflareInfo(ip)
                    val status = NetworkUtils.checkDataExchange(ip, port)
                    val isVerified = status == "تبادل موفق"

                    withContext(Dispatchers.Main) {
                        val index = foundIps.indexOfFirst { it.ip == ip }
                        if (index != -1) {
                            foundIps[index] = foundIps[index].copy(
                                colo = info.first,
                                countryCode = info.second,
                                exchangeStatus = status,
                                packetLoss = if (!isVerified) 100 else foundIps[index].packetLoss
                            )
                            resortFoundIps()
                        }
                    }

                    if (isVerified) {
                        speedQueue.trySend(ip)
                        // اسکن همسایه‌های همین بلوک /24 (کیفیت مشابه در کلودفلر رایج است)
                        if (!isNeighbor && foundIps.size < maxResults) {
                            val octets = ip.split(".")
                            if (octets.size == 4) {
                                val base = octets[3].toIntOrNull() ?: 0
                                val neighborOffsets = listOf(-2, -1, 1, 2)
                                neighborOffsets.forEach { offset ->
                                    val lastOctet = base + offset
                                    if (lastOctet in 1..254) {
                                        val neighborIp = "${octets[0]}.${octets[1]}.${octets[2]}.$lastOctet"
                                        launch { semaphore.withPermit { probeIp(neighborIp, port, isNeighbor = true) } }
                                    }
                                }
                            }
                        }
                    }
                }

                // حلقه اصلی فاز ۱: پروب تصادفی
                while (isScanning.value && foundIps.size < maxResults) {
                    launch {
                        semaphore.withPermit {
                            val range = if (selectedRanges.isEmpty()) ipRanges.random() else selectedRanges.random()
                            val ip = generateSmartIp(range)
                            probeIp(ip, userPort, isNeighbor = false)
                        }
                    }
                    delay(15) // وقفه کوتاه برای مدیریت پردازش
                }
                isScanning.value = false
            }

            speedQueue.close()
            speedWorkers.joinAll()
            isSpeedTesting = false
            speedTestProgress = ""
        }
    }

    /**
     * مرتب‌سازی نهایی نتایج: ابتدا تبادل موفق، سپس IPهایی که تست سرعت شده‌اند با سرعت دانلود بالاتر،
     * و در آخر بر اساس کمترین پکت‌لاست/پینگ.
     */
    private fun resortFoundIps() {
        val successLabel = "تبادل موفق"
        foundIps.sortWith(
            compareByDescending<IpScanResult> { it.exchangeStatus == successLabel }
                .thenByDescending { it.isSpeedTested }
                .thenByDescending { it.downloadMbps }
                .thenBy { it.packetLoss }
                .thenBy { it.latency }
        )
    }

    // متد بهبود یافته برای تولید آی‌پی‌های متنوع‌تر در رنج
    private fun generateSmartIp(range: String): String {
        val parts = range.split("/")[0].split(".")
        val mask = try {
            range.split("/")[1].toInt()
        } catch (e: Exception) {
            24
        }

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

    private suspend fun checkSocket(ip: String, port: Int, timeout: Int) =
        withContext(Dispatchers.IO) {
            var successfulAttempts = 0
            val totalAttempts = 5
            var totalLatency = 0L

            for (i in 1..totalAttempts) {
                val start = System.currentTimeMillis()
                try {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(ip, port), timeout)
                    }
                    successfulAttempts++
                    totalLatency += (System.currentTimeMillis() - start)
                } catch (e: Exception) {
                    // این تلاش شکست خورد - سوکت در use{} حتی روی استثنا بسته می‌شود
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

    fun startDeepFragmentScan(targetHost: String = "1.1.1.1") {
        viewModelScope.launch(Dispatchers.IO) {
            isScanningg = true
            currentProgress = 0f
            scanResults.clear()

            currentTestInfo = "در حال بررسی سلامت IP..."
            if (!checkServerHealth(targetHost)) {
                currentTestInfo = "❌ سرور در دسترس نیست."
                isScanningg = false
                return@launch
            }

            // بازه‌های تست (بر اساس تجربه‌های موفق در ایران)
            val lengths = listOf(10, 20, 30, 40, 50, 80, 100, 150, 200)
            val intervals = listOf(1, 2, 5, 10, 15, 20, 30, 50)

            val totalSteps = lengths.size * intervals.size
            var completedSteps = 0

            for (len in lengths) {
                for (inter in intervals) {
                    if (!isScanningg) return@launch
                    currentTestInfo = "تست پکت: $len بایت | تاخیر: $inter میلی‌ثانیه"

                    val result = performProfessionalTest(targetHost, 443, len, inter)
                    if (result != null && result.stability > 50) {
                        withContext(Dispatchers.Main) {
                            scanResults.add(result)
                            scanResults.sortWith(compareByDescending<FragmentResult> { it.stability }.thenBy { it.latency })
                        }
                    }
                    completedSteps++
                    currentProgress = completedSteps.toFloat() / totalSteps
                }
            }
            currentTestInfo = "اسکن با موفقیت تمام شد ✅"
            isScanningg = false
        }
    }

    private fun performProfessionalTest(
        host: String,
        port: Int,
        len: Int,
        inter: Int
    ): FragmentResult? {
        val start = System.currentTimeMillis()
        var successCount = 0
        val retryCount = 3 // تست ۳ مرحله‌ای برای تعیین دقیق پایداری

        // شبیه‌سازی پکت TLS Client Hello
        val tlsData = ByteArray(250) { it.toByte() }

        try {
            repeat(retryCount) {
                java.net.Socket().use { socket ->
                    socket.connect(java.net.InetSocketAddress(host, port), 1500)
                    val out = socket.outputStream

                    // خرد کردن پکت دقیقا مشابه موتور V2Ray
                    tlsData.toList().chunked(len).forEach { chunk ->
                        out.write(chunk.toByteArray())
                        out.flush()
                        if (inter > 0) Thread.sleep(inter.toLong())
                    }

                    socket.soTimeout = 1000
                    if (socket.inputStream.read() != -1) successCount++
                }
            }

            if (successCount > 0) {
                val latency = (System.currentTimeMillis() - start) / retryCount
                val stability = (successCount.toFloat() / retryCount * 100).toInt()

                // ایجاد بازه‌های پیشنهادی برای فیلد V2Ray
                val lRange = if (len <= 20) "1-$len" else "${len - 15}-$len"
                val iRange = if (inter <= 5) "1-$inter" else "${inter / 2}-$inter"

                return FragmentResult(lRange, iRange, len, inter, latency, stability)
            }
        } catch (e: Exception) {
            return null
        }
        return null
    }

    private fun checkServerHealth(host: String): Boolean {
        return try {
            java.net.Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress(host, 443), 2000)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun stopScan() {
        isScanningg = false
        currentTestInfo = "اسکن متوقف شد"
    }


    fun runFullDiagnostics(targetHost: String = "1.1.1.1") {
        viewModelScope.launch(Dispatchers.IO) {
            isAnalyzing = true
            analysisResults.clear()

            // مرحله ۱: اینترنت
            analysisResults.add(
                AnalysisStep(
                    "اتصال اینترنت",
                    AnalysisStatus.LOADING,
                    "در حال بررسی..."
                )
            )
            val hasNet = checkInternet()
            updateStep(
                0, if (hasNet) AnalysisStatus.SUCCESS else AnalysisStatus.ERROR,
                if (hasNet) "اینترنت وصل است." else "شما به اینترنت متصل نیستید!"
            )

            if (!hasNet) {
                isAnalyzing = false; return@launch
            }

            // مرحله ۲: ساعت سیستم
            analysisResults.add(
                AnalysisStep(
                    "ساعت سیستم",
                    AnalysisStatus.LOADING,
                    "در حال بررسی..."
                )
            )
            val timeDiff = checkSystemTime()
            updateStep(
                1, if (timeDiff < 30000) AnalysisStatus.SUCCESS else AnalysisStatus.ERROR,
                if (timeDiff < 30000) "ساعت دقیق است." else "اختلال در ساعت (بیش از ۳۰ ثانیه اختلاف)!"
            )

            // مرحله ۳: DNS
            analysisResults.add(
                AnalysisStep(
                    "وضعیت DNS",
                    AnalysisStatus.LOADING,
                    "در حال بررسی..."
                )
            )
            val dnsOk = checkDNS()
            updateStep(
                2, if (dnsOk) AnalysisStatus.SUCCESS else AnalysisStatus.WARNING,
                if (dnsOk) "DNS سالم است." else "اختلال در DNS (احتمال فیلترینگ DNS)."
            )

            // مرحله ۴: سلامت سرور
            analysisResults.add(
                AnalysisStep(
                    "وضعیت سرور",
                    AnalysisStatus.LOADING,
                    "در حال بررسی..."
                )
            )
            val serverAlive = checkServerHealth(targetHost)
            updateStep(
                3, if (serverAlive) AnalysisStatus.SUCCESS else AnalysisStatus.ERROR,
                if (serverAlive) "سرور در دسترس است." else "سرور پاسخ نمی‌دهد (احتمال فیلتر IP)."
            )

            isAnalyzing = false
        }
    }

    private fun updateStep(index: Int, status: AnalysisStatus, message: String) {
        if (index < analysisResults.size) {
            analysisResults[index] = analysisResults[index].copy(status = status, message = message)
        }
    }

    // --- توابع اجرایی تست‌ها ---

    private fun checkInternet(): Boolean {
        // چند هاست مختلف امتحان می‌شود تا بلاک‌شدن یک سرویس خاص باعث نتیجه غلط نشود
        val hosts = listOf("8.8.8.8", "1.1.1.1")
        for (host in hosts) {
            try {
                java.net.Socket().use { socket ->
                    socket.connect(java.net.InetSocketAddress(host, 53), 1500)
                }
                return true
            } catch (e: Exception) {
                // امتحان هاست بعدی
            }
        }
        return false
    }

    private fun checkDNS(): Boolean {
        val domains = listOf("google.com", "cloudflare.com")
        for (domain in domains) {
            try {
                val address = java.net.InetAddress.getByName(domain)
                if (address.hostAddress.isNotEmpty()) return true
            } catch (e: Exception) {
                // امتحان دامنه بعدی
            }
        }
        return false
    }

    private fun checkSystemTime(): Long {
        // در حالت ایده آل باید با سرور NTP چک شود،
        // فعلاً برای سادگی فرض میکنیم اگر DNS و اینترنت وصل باشد، زمان سیستم را با زمان دریافت شده از هدر یک سایت مقایسه میکنیم.
        // اگر یک سایت بلاک باشد، سایت بعدی هم امتحان می‌شود.
        val hosts = listOf("https://google.com", "https://cloudflare.com")
        for (urlStr in hosts) {
            try {
                val url = java.net.URL(urlStr)
                val connection = url.openConnection()
                connection.connectTimeout = 2000
                val serverDate = connection.getHeaderFieldDate("Date", 0)
                if (serverDate != 0L) return Math.abs(System.currentTimeMillis() - serverDate)
            } catch (e: Exception) {
                // امتحان سایت بعدی
            }
        }
        return 0L
    }


}

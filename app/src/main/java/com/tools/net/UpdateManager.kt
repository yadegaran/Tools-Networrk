package com.tools.net

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

// مدل داده ورژن


class UpdateManager {
    object updateManager {
        private const val UPDATE_URL =
            "https://raw.githubusercontent.com/yadegaran/Tools-Networrk/refs/heads/master/update.json"


        suspend fun fetchUpdateInfo(): UpdateInfo? {
            return withContext(Dispatchers.IO) {
                val client = OkHttpClient()
                val url = "https://raw.githubusercontent.com/yadegaran/Tools-Networrk/refs/heads/master/update.json?t=${System.currentTimeMillis()}"
                try {
                    val request = Request.Builder().url(url).build()
                    val response = client.newCall(request).execute()
                    val jsonData = response.body?.string()

                    // این دو خط لاگ رو اضافه کن:
                    android.util.Log.d("UpdateCheck", "JSON Received: $jsonData")

                    if (jsonData == null) return@withContext null
                    val obj = JSONObject(jsonData)
                    UpdateInfo(
                        versionCode = obj.getInt("versionCode"),
                        downloadUrl = obj.getString("downloadUrl"),
                        changeLog = obj.getString("changeLog")
                    )
                } catch (e: Exception) {
                    // این لاگ خطا رو نشون میده (مثلاً مشکل SSL یا DNS)
                    android.util.Log.e("UpdateCheck", "Error: ${e.message}")
                    null
                }
            }
        }

        fun getCurrentVersionCode(context: Context): Int {
            return try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode.toInt()
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode
                }
            } catch (e: Exception) {
                1
            }
        }

        fun getAppVersionName(context: Context): String {
            return try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
            } catch (e: Exception) {
                "1.0.0"
            }
        }

        fun startDownload(context: Context, url: String) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
                Toast.makeText(context, "در حال انتقال به مرورگر...", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "خطا در باز کردن لینک دانلود!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
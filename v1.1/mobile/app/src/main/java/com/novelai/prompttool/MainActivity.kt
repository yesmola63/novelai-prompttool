package com.novelai.prompttool

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var server: PromptsServer
    private var multiFilePathCallback: ValueCallback<Array<Uri>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        server = PromptsServer(this, 0)
        server.start(2000)
        val port = server.listeningPort

        val webView = findViewById<WebView>(R.id.webView)
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = false
        settings.cacheMode = WebSettings.LOAD_NO_CACHE
        // 本地页面已有 viewport meta，禁用宽视口/总览模式：
        // 这两个设置在部分设备上滚动时会触发页面缩放跳动（"往下翻时往上飘"）
        settings.loadWithOverviewMode = false
        settings.useWideViewPort = false
        // 禁用缩放（双指/双击），避免滚动中误触缩放导致内容跳动
        settings.setSupportZoom(false)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
        // 强制硬件加速渲染层，减少滚动掉帧
        webView.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
        webView.setBackgroundColor(android.graphics.Color.TRANSPARENT)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
            }
        }

        // 关闭过度滚动回弹（Android 上滚动到边界时的橡皮筋效果会造成"往上飘"的错觉）
        webView.overScrollMode = android.view.View.OVER_SCROLL_NEVER

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                multiFilePathCallback = filePathCallback
                // 直接打开相册选择图片
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(intent, REQUEST_IMAGE_PICKER)
                return true
            }
        }

        val baseUrl = "http://127.0.0.1:$port/"
        webView.loadUrl(baseUrl)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_IMAGE_PICKER) {
            val callback = multiFilePathCallback
            multiFilePathCallback = null
            if (callback == null) return
            val uris: Array<Uri> = when {
                resultCode == Activity.RESULT_OK && data != null -> {
                    val clipData = data.clipData
                    if (clipData != null && clipData.itemCount > 0) {
                        Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
                    } else {
                        arrayOf(data.data ?: Uri.EMPTY)
                    }
                }
                else -> emptyArray()
            }
            callback.onReceiveValue(uris)
            return
        }

        if (requestCode == REQUEST_LOAD_FILE_PICKER) {
            handleLoadFilePickerResult(resultCode, data)
            return
        }

        if (requestCode == REQUEST_DIR_PICKER) {
            handleDirPickerResult(resultCode, data)
            return
        }
    }

    private fun handleLoadFilePickerResult(resultCode: Int, data: Intent?) {
        val future = server.getFilePickerFuture()
        // Sequence guard: if a newer picker was launched while this one was open,
        // the previous future was already overwritten — discard the stale result.
        if (future == null) {
            Log.i("MainActivity", "load_file_picker_result: no_pending_future (seq=${server.getExpectedPickerSeq()})")
            return
        }

        if (resultCode != Activity.RESULT_OK || data == null || data.data == null) {
            Log.i("MainActivity", "load_file_picker_result: cancelled")
            future.complete(
                FilePickerResult(success = false, error = null)
            )
            return
        }

        val uri = data.data!!
        try {
            val fileName = queryFileName(uri)
            val cacheSubDir = File(cacheDir, "load_cache")
            cacheSubDir.mkdirs()
            val copyFile = File(cacheSubDir, fileName)
            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.w("MainActivity", "load_file_picker_result: cannot_open_uri")
                future.complete(
                    FilePickerResult(success = false, error = "无法读取文件内容")
                )
                return
            }
            java.io.FileOutputStream(copyFile).use { os ->
                inputStream.use { input -> input.copyTo(os) }
            }

            // Re-check future is still the same one (another pick may have replaced it mid-flight)
            if (server.getFilePickerFuture() !== future) {
                Log.i("MainActivity", "load_file_picker_result: stale_future_dropped (seq=${server.getExpectedPickerSeq()})")
                copyFile.delete()
                return
            }

            future.complete(
                FilePickerResult(
                    success = true,
                    path = copyFile.absolutePath,
                    filename = fileName,
                    valid = true
                )
            )
            Log.i("MainActivity", "load_file_picker_result: completed path=${copyFile.absolutePath} seq=${server.getExpectedPickerSeq()}")
        } catch (e: Exception) {
            Log.w("MainActivity", "load_file_picker_result: exception", e)
            future.complete(
                FilePickerResult(success = false, error = e.message ?: "选择文件失败")
            )
        }
    }

    private fun queryFileName(uri: Uri): String {
        var fileName = "selected_bundle.bundle"
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                fileName = it.getString(nameIndex)
            }
        }
        if (fileName.isEmpty()) {
            fileName = uri.lastPathSegment ?: "selected_bundle.bundle"
        }
        return if (fileName.endsWith(".bundle", ignoreCase = true)) fileName
               else if (fileName.endsWith(".zip", ignoreCase = true)) fileName + ".bundle"
               else fileName + ".bundle"
    }

    private fun handleDirPickerResult(resultCode: Int, data: Intent?) {
        val future = server.getDirPickerFuture()
        if (future == null) return
        if (resultCode != Activity.RESULT_OK || data == null || data.data == null) {
            future.complete("")
            return
        }
        // ACTION_CREATE_DOCUMENT 直接返回可写入的文档 URI，无需额外权限
        future.complete(data.data!!.toString())
    }

    override fun onBackPressed() {
        val wv = findViewById<WebView>(R.id.webView)
        wv.evaluateJavascript(
            "typeof closeTopModal === 'function' && closeTopModal()",
            { result ->
                if (result == "true") return@evaluateJavascript
                super.onBackPressed()
            }
        )
    }

    // ===== 应用内更新安装 =====
    // 需要「安装未知应用」权限时先跳系统设置，用户授权返回后自动继续安装
    var pendingInstallApk: File? = null

    override fun onResume() {
        super.onResume()
        val apk = pendingInstallApk ?: return
        if (!apk.exists()) {
            pendingInstallApk = null
            return
        }
        val granted = android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O ||
                packageManager.canRequestPackageInstalls()
        if (granted) {
            pendingInstallApk = null
            launchInstaller(apk)
        }
    }

    fun launchInstaller(apkFile: File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this, "$packageName.fileprovider", apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            Log.i("MainActivity", "installer launched: ${apkFile.name}")
        } catch (e: Exception) {
            Log.e("MainActivity", "launchInstaller failed", e)
        }
    }

    override fun onDestroy() {
        server.stop()
        super.onDestroy()
    }

    companion object {
        const val REQUEST_IMAGE_PICKER = 1001
        const val REQUEST_LOAD_FILE_PICKER = 1002
        const val REQUEST_DIR_PICKER = 1003
    }
}

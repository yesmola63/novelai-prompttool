package com.novelai.prompttool

import android.content.Context
import android.content.Intent
import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.graphics.BitmapFactory
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Method
import fi.iki.elonen.NanoHTTPD.Response.Status
import fi.iki.elonen.NanoHTTPD.IHTTPSession
import fi.iki.elonen.NanoHTTPD.Response
import java.io.*
import java.nio.file.Files
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.HashMap
import org.json.JSONObject

private const val TAG = "PromptsServer"

data class FilePickerResult(
    val success: Boolean = false,
    val path: String = "",
    val filename: String = "",
    val error: String? = null,
    val valid: Boolean = false,
    val needPassword: Boolean = false
)

class PromptsServer(val activity: MainActivity, port: Int = 8080) : NanoHTTPD(port) {

    private val context = activity
    private val appContext: Context = activity
    private val dataDir: File = appContext.getExternalFilesDir(null) ?: appContext.filesDir
    private val promptManager = PromptManager(appContext)

    private val filePickerFuture: AtomicReference<CompletableFuture<FilePickerResult>> =
        AtomicReference(null)
    private val filePickerSeq = AtomicInteger(0) // monotonic; guards against stale picker results
    private val dirPickerFuture: AtomicReference<CompletableFuture<String>> =
        AtomicReference(null)
    private val selectedLoadFilePath: AtomicReference<String> = AtomicReference("")
    private val selectedLoadPathDecrypted: AtomicReference<String> = AtomicReference("")
    private val selectedPackDirPath: AtomicReference<String> = AtomicReference("")
    private val selectedPackDirUri: AtomicReference<String> = AtomicReference("")

    private val packResults: ConcurrentHashMap<String, JSONObject> = ConcurrentHashMap()
    private val loadResults: ConcurrentHashMap<String, JSONObject> = ConcurrentHashMap()

    // ===== 应用更新相关状态 =====
    private val updateApiUrl = "https://novelai-prompttool-update.arisuhhome.xyz/update"
    @Volatile private var updateDownloading = false
    @Volatile private var updateDone = false
    @Volatile private var updateSuccess = false
    @Volatile private var updateError: String? = null
    @Volatile private var updateDownloaded = 0L
    @Volatile private var updateTotal = 0L
    @Volatile private var updateSpeed = 0.0
    @Volatile private var updateApkPath: String = ""

    fun getFilePickerFuture(): CompletableFuture<FilePickerResult>? = filePickerFuture.get()
    fun getDirPickerFuture(): CompletableFuture<String>? = dirPickerFuture.get()
    fun getExpectedPickerSeq(): Int = filePickerSeq.get()

    fun triggerFilePicker() {
        activity.runOnUiThread {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            activity.startActivityForResult(intent, MainActivity.REQUEST_LOAD_FILE_PICKER)
        }
    }

    fun triggerDirPicker(filename: String = "prompt.bundle") {
        activity.runOnUiThread {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_TITLE, filename)
            }
            activity.startActivityForResult(intent, MainActivity.REQUEST_DIR_PICKER)
        }
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method

        if (uri.startsWith("/picture/")) {
            return servePicture(uri)
        }

        if (uri.startsWith("/api/")) {
            return handleApi(session, uri, method)
        }

        return serveAsset(uri)
    }

    private fun servePicture(uri: String): Response {
        // 支持 ?w=400 缩略图参数：卡片用缩略图，详情用原图
        var rawPath = uri.substring("/picture/".length)
        var thumbWidth = 0
        val qIdx = rawPath.indexOf('?')
        if (qIdx >= 0) {
            val query = rawPath.substring(qIdx + 1)
            rawPath = rawPath.substring(0, qIdx)
            thumbWidth = Regex("w=(\\d+)").find(query)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        }
        val filename = rawPath
        val decodedFilename = try {
            java.net.URLDecoder.decode(filename, Charsets.UTF_8.name())
        } catch (e: Exception) { filename }

        val searchPaths = listOf(
            File(dataDir, "picture/$decodedFilename"),
            File(dataDir, "picture/$filename"),
            File(appContext.filesDir, "picture/$decodedFilename"),
            File(appContext.filesDir, "picture/$filename")
        )

        var imgFile: File? = null
        for (candidate in searchPaths) {
            if (candidate.exists()) { imgFile = candidate; break }
        }

        if (imgFile == null) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json",
                "{\"error\":\"image_not_found\"}")
        }
        val ext = filename.substringAfterLast('.', "").lowercase()
        val mimeType = when (ext) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "bmp" -> "image/bmp"
            else -> "image/octet-stream"
        }

        // 缩略图：生成小尺寸 JPEG 并缓存，滚动时加载快、不卡顿
        if (thumbWidth in 32..800) {
            return serveThumbnail(imgFile, thumbWidth, mimeType)
        }

        return newFixedLengthResponse(Response.Status.OK, mimeType,
            FileInputStream(imgFile), imgFile.length())
    }

    private fun serveThumbnail(imgFile: File, thumbWidth: Int, mimeType: String): Response {
        val cacheDir = File(dataDir, "thumb_cache")
        cacheDir.mkdirs()
        val cacheFile = File(cacheDir, "${imgFile.name}_w${thumbWidth}.jpg")
        if (cacheFile.exists() && cacheFile.length() > 0) {
            return newFixedLengthResponse(Response.Status.OK, "image/jpeg",
                FileInputStream(cacheFile), cacheFile.length())
        }
        try {
            // 先读边界，计算采样率，避免大图完整解码
            val boundsOpts = BitmapFactory.Options()
            boundsOpts.inJustDecodeBounds = true
            BitmapFactory.decodeFile(imgFile.absolutePath, boundsOpts)
            var sample = 1
            var w = boundsOpts.outWidth
            while (w / (sample * 2) >= thumbWidth && sample * 2 <= 16) {
                sample *= 2
            }
            val opts = BitmapFactory.Options()
            opts.inSampleSize = sample
            val bmp = BitmapFactory.decodeFile(imgFile.absolutePath, opts) ?: run {
                return newFixedLengthResponse(Response.Status.OK, mimeType,
                    FileInputStream(imgFile), imgFile.length())
            }
            val scaled = android.graphics.Bitmap.createScaledBitmap(bmp,
                thumbWidth, (bmp.height * thumbWidth / bmp.width).coerceAtLeast(1), true)
            if (scaled !== bmp) bmp.recycle()
            val tmp = File(cacheDir, "${imgFile.name}_w${thumbWidth}.tmp")
            FileOutputStream(tmp).use { out ->
                scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 82, out)
            }
            scaled.recycle()
            if (tmp.renameTo(cacheFile)) {
                return newFixedLengthResponse(Response.Status.OK, "image/jpeg",
                    FileInputStream(cacheFile), cacheFile.length())
            }
        } catch (e: Exception) {
            Log.w(TAG, "thumbnail failed: ${e.message ?: "error"}")
        }
        // 降级：直接返回原图
        return newFixedLengthResponse(Response.Status.OK, mimeType,
            FileInputStream(imgFile), imgFile.length())
    }

    private fun handleApi(session: IHTTPSession, uri: String, method: Method): Response {
        return try {
            when {
                uri == "/api/prompts" && method == Method.GET ->
                    newFixedLengthResponse(Response.Status.OK, "application/json", promptManager.loadPrompts())

                uri == "/api/prompts/check-empty" && method == Method.GET -> {
                    val data = promptManager.loadPrompts()
                    val empty = data == "{}" || data.trim() == "{}"
                    newFixedLengthResponse(Response.Status.OK, "application/json", "{\"empty\":$empty}")
                }

                uri == "/api/prompts" && method == Method.POST -> {
                    val body = readJsonBody(session)
                    val group = body.optString("group")
                    if (group.isEmpty()) return jsonResp(400, "{\"error\":\"missing_group\"}")
                    val item = body.optJSONObject("item")
                        ?: return jsonResp(400, "{\"error\":\"missing_item\"}")
                    promptManager.addPrompt(group, item)
                    jsonResp(201, "{\"message\":\"added\"}")
                }

                uri == "/api/prompts" && method == Method.PUT -> {
                    val body = readJsonBody(session)
                    val og = body.optString("originalGroup")
                    val or = body.optString("originalRole")
                    val oc = body.optString("originalCommon")
                    val ng = body.optString("newGroup")
                    val ni = body.optJSONObject("newItem")
                    if (og.isEmpty() || or.isEmpty() || oc.isEmpty() || ng.isEmpty() || ni == null) {
                        return jsonResp(400, "{\"error\":\"missing_params\"}")
                    }
                    promptManager.updatePrompt(og, or, oc, ng, ni)
                    jsonResp(200, "{\"message\":\"updated\"}")
                }

                uri.startsWith("/api/prompts") && method == Method.DELETE -> {
                    val params = session.parms
                    val id = params["id"]
                    if (id != null) {
                        promptManager.deletePromptById(id)
                    } else {
                        val g = params["group"] ?: return jsonResp(400, "{\"error\":\"missing_params\"}")
                        val r = params["role"] ?: return jsonResp(400, "{\"error\":\"missing_params\"}")
                        val c = params["common"] ?: return jsonResp(400, "{\"error\":\"missing_params\"}")
                        promptManager.deletePrompt(g, r, c)
                    }
                    jsonResp(200, "{\"message\":\"deleted\"}")
                }

                uri == "/api/upload-images" && method == Method.POST -> {
                    handleUploadImages(session)
                }

                uri == "/api/images" && method == Method.DELETE -> {
                    val body = readJsonBody(session)
                    val filenames = body.optJSONArray("filenames")
                        ?: return jsonResp(400, "{\"error\":\"no_filenames\"}")
                    val picDir = File(dataDir, "picture")
                    val deleted = java.util.ArrayList<String>()
                    for (i in 0 until filenames.length()) {
                        val name = filenames.getString(i)
                        val file = File(picDir, name)
                        if (file.exists()) { file.delete(); deleted.add(name) }
                    }
                    jsonResp(200, "{\"deleted\":${fastJsonArr(deleted)}}")
                }

                uri == "/api/prompts/replace" && method == Method.PUT -> {
                    val body = readJsonBody(session)
                    promptManager.savePrompts(body.toString(2))
                    jsonResp(200, "{\"message\":\"updated\"}")
                }

                uri == "/api/prompt/recovery-check" && method == Method.GET ->
                    jsonResp(200, "{\"needRecovery\":false,\"params\":null}")

                uri == "/api/prompt/recover" && method == Method.POST ->
                    jsonResp(400, "{\"error\":\"not_supported\"}")

                uri == "/api/pack" && method == Method.POST -> handlePack(session)
                uri == "/api/pack/choose-dir" && method == Method.POST -> handleChooseDir(session)
                uri.startsWith("/api/pack/choose-dir") && method == Method.GET -> handlePollChooseDir(session)
                uri == "/api/pack/check-filename" && method == Method.POST -> handleCheckPackFilename(session)
                uri.startsWith("/api/pack/progress") -> {
                    val params = session.parms
                    val taskId = params["task_id"] ?: ""
                    val result = if (taskId.isNotEmpty()) packResults[taskId] else null
                    if (result != null) {
                        Log.i(TAG, "pack_progress_poll task=$taskId")
                        jsonResp(200, result.toString())
                    } else {
                        Log.i(TAG, "pack_progress_poll_unknown_task taskId=$taskId")
                        jsonResp(404, "{\"error\":\"unknown_task\"}")
                    }
                }

                uri == "/api/load/select-file" && method == Method.POST -> handleSelectLoadFile(session)
                uri.startsWith("/api/load/select-file") && method == Method.GET -> handlePollSelectLoadFile(session)
                uri == "/api/load/unlock" && method == Method.POST -> handleUnlockLoadFile(session)
                uri == "/api/load/apply" && method == Method.POST -> handleApplyLoad(session)
                uri.startsWith("/api/load/progress") -> {
                    val params = session.parms
                    val taskId = params["task_id"] ?: ""
                    val result = if (taskId.isNotEmpty()) loadResults[taskId] else null
                    if (result != null) {
                        Log.i(TAG, "load_progress_poll task=$taskId status=${result.optString("status","?")} done=${result.optBoolean("done",false)}")
                        jsonResp(200, result.toString())
                    } else {
                        Log.i(TAG, "load_progress_poll_unknown_task taskId=$taskId")
                        jsonResp(404, "{\"error\":\"unknown_task\"}")
                    }
                }

                uri == "/api/save-image" && method == Method.POST -> handleSaveImage(session)

                // ===== 应用信息 / 更新 =====
                uri == "/api/app-info" && method == Method.GET -> handleAppInfo()
                uri == "/api/update/check" && method == Method.GET -> handleUpdateCheck()
                uri == "/api/update/pending" && method == Method.GET -> handleUpdatePending()
                uri == "/api/update/snooze" && method == Method.POST -> handleUpdateSnooze()
                uri == "/api/update/download" && method == Method.POST -> handleUpdateDownload(session)
                uri == "/api/update/progress" && method == Method.GET -> handleUpdateProgress()
                uri == "/api/update/install" && method == Method.POST -> handleUpdateInstall()

                else -> jsonResp(404, "{\"error\":\"Not found\"}")
            }
        } catch (e: Exception) {
            val msg = e.message ?: "error"
            Log.e(TAG, "API error: $uri $msg", e)
            jsonResp(500, "{\"error\":\"$msg\"}")
        }
    }

    private fun handleUploadImages(session: IHTTPSession): Response {
        val picDir = File(dataDir, "picture")
        picDir.mkdirs()
        val filenames = java.util.ArrayList<String>()

        try {
            val contentType = session.headers["content-type"] ?: ""

            if (!contentType.contains("multipart/form-data")) {
                return jsonResp(400, "{\"error\":\"no_file_uploaded\"}")
            }

            val params = HashMap<String, String>()
            session.parseBody(params)

            for ((key, value) in params) {
                if (value.isEmpty()) continue
                val sourceFile = File(value)
                if (!sourceFile.exists()) continue

                val sha1 = sha1HexStream(sourceFile.inputStream())
                val ext = guessExtensionStream(sourceFile.inputStream())
                val filename = "$sha1.$ext"
                val dest = File(picDir, filename)
                if (!dest.exists()) {
                    copyFileStream(sourceFile, dest)
                }
                filenames.add(filename)
            }
        } catch (e: Exception) {
            val msg = e.message ?: "upload_failed"
            Log.e(TAG, "upload_failed", e)
            return jsonResp(500, "{\"error\":\"$msg\"}")
        }

        return jsonResp(200, "{\"filenames\":${fastJsonArr(filenames)}}")
    }

    private fun handleCheckPackFilename(session: IHTTPSession): Response {
        return try {
            val body = readJsonBody(session)
            val filename = body.optString("filename", "").trim().ifEmpty { "prompt" }
            val downloadDir = File(android.os.Environment.getExternalStorageDirectory(), "Download/prompt_pack")
            val target = File(downloadDir, "$filename.bundle")
            val exists = target.exists()
            jsonResp(200, "{\"exists\":$exists,\"path\":\"${escapeJson(target.absolutePath)}\"}")
        } catch (e: Exception) {
            jsonResp(200, "{\"exists\":false}")
        }
    }

    private fun handlePack(session: IHTTPSession): Response {
        val body = readJsonBody(session)
        val filename = body.optString("filename").trim().ifEmpty { "prompt" }
        val selectedIds = body.optJSONArray("selectedIds")
        val ids = if (selectedIds != null) {
            val list = java.util.ArrayList<String>()
            for (i in 0 until selectedIds.length()) list.add(selectedIds.getString(i))
            list
        } else null
        val needPassword = body.optBoolean("needPassword", false)
        val password = body.optString("password", "")
        val imageMode = body.optString("imageMode", "none")
        val webpLossless = body.optBoolean("webpLossless", false)
        val webpQuality = body.optInt("webpQuality", 80)
        val zipMode = body.optString("zipMode", "standard")

        val selectedUri = selectedPackDirUri.get().takeIf { it.isNotBlank() }
        selectedPackDirUri.set("")

        val taskId = UUID.randomUUID().toString()
        packResults[taskId] = JSONObject().put("status", "pending").put("text", "packing...")

        Thread {
            try {
                val tempDir = File(dataDir, "temp_pack_$taskId")
                deleteDir(tempDir)
                tempDir.mkdirs()

                promptManager.packPrompts(filename, ids, null)
                val srcBundle = File(dataDir, PACK_DIR_NAME + "/" + filename + ".bundle")
                extractZip(srcBundle, tempDir)

                val picTemp = File(tempDir, "picture")

                if (imageMode == "webp" && picTemp.exists()) {
                    val pngs = picTemp.listFiles { _, n -> n.lowercase().endsWith(".png") } ?: emptyArray()
                    val mapping = HashMap<String, String>()
                    val promptFile = File(tempDir, "prompt.json")
                    var promptText = if (promptFile.exists()) promptFile.readText() else "{}"
                    for (png in pngs) {
                        val webpName = png.name.substringBeforeLast(".") + ".webp"
                        val webpFile = File(picTemp, webpName)
                        try {
                            convertToWebp(png, webpFile, webpLossless, webpQuality)
                            mapping[png.name] = webpName
                        } catch (e: Exception) {
                            Log.w(TAG, "webp_fail ${png.name}")
                        }
                    }
                    if (mapping.isNotEmpty()) {
                        for ((oldName, newName) in mapping) {
                            promptText = promptText.replace("\"$oldName\"", "\"$newName\"")
                        }
                        promptFile.writeText(promptText)
                    }
                }

                packResults[taskId] = JSONObject().put("status", "pending").put("text", "compressing...")
                val destFile = File(dataDir, PACK_DIR_NAME + "/" + filename + ".bundle")
                if (destFile.exists()) destFile.delete()

                if (needPassword) {
                    packResults[taskId] = JSONObject().put("status", "pending").put("text", "encrypting...")
                    compressTo7z(tempDir, destFile, password)
                } else if (imageMode == "zip") {
                    compressToZip(tempDir, destFile, zipMode)
                } else {
                    compressToZipStored(tempDir, destFile)
                }

                if (selectedUri != null && destFile.exists()) {
                    try {
                        val outStream = activity.contentResolver.openOutputStream(android.net.Uri.parse(selectedUri))
                        if (outStream != null) {
                            java.io.FileInputStream(destFile).use { inS ->
                                outStream.use { out -> inS.copyTo(out) }
                            }
                            packResults[taskId] = JSONObject().put("done", true)
                                .put("success", true).put("message", "$filename.bundle")
                        } else {
                            packResults[taskId] = JSONObject().put("done", true)
                                .put("success", true).put("message", destFile.absolutePath)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "SAF failed")
                        packResults[taskId] = JSONObject().put("done", true)
                            .put("success", true).put("message", destFile.absolutePath)
                    }
                } else {
                    // 尝试写入 Download/prompt_pack/
                    val downloadDir = File(android.os.Environment.getExternalStorageDirectory(), "Download/prompt_pack")
                    try {
                        downloadDir.mkdirs()
                        val targetFile = File(downloadDir, "$filename.bundle")
                        java.io.FileInputStream(destFile).use { inS ->
                            java.io.FileOutputStream(targetFile).use { outS ->
                                inS.copyTo(outS)
                            }
                        }
                        packResults[taskId] = JSONObject().put("done", true)
                            .put("success", true).put("message", targetFile.absolutePath)
                    } catch (e: Exception) {
                        Log.w(TAG, "Download failed: ${e.message ?: "error"}")
                        packResults[taskId] = JSONObject().put("done", true)
                            .put("success", true).put("message", destFile.absolutePath)
                    }
                }

                // 清理应用外部存储目录内的打包缓存文件
                val localPackDir = File(dataDir, PACK_DIR_NAME)
                if (localPackDir.exists()) {
                    deleteDir(localPackDir)
                    Log.i(TAG, "cleaned local pack dir: ${localPackDir.absolutePath}")
                }

                deleteDir(tempDir)
            } catch (e: Exception) {
                val msg = e.message ?: "pack_failed"
                packResults[taskId] = JSONObject().put("done", true)
                    .put("success", false).put("error", msg)
                Log.e(TAG, "pack_failed", e)
            }
        }.start()

        return jsonResp(200, "{\"task_id\":\"$taskId\"}")
    }

    private fun convertToWebp(src: File, dest: File, lossless: Boolean, quality: Int) {
        val bitmap = BitmapFactory.decodeStream(FileInputStream(src))
            ?: throw RuntimeException("cannot_decode: ${src.name}")
        try {
            FileOutputStream(dest).use { out ->
                bitmap.compress(
                    if (lossless) android.graphics.Bitmap.CompressFormat.WEBP_LOSSLESS
                    else android.graphics.Bitmap.CompressFormat.WEBP,
                    quality.coerceIn(0, 100), out)
            }
        } finally { bitmap.recycle() }
        src.delete()
    }

    private fun compressTo7z(sourceDir: File, destFile: File, password: String) {
        compressToZipEncrypted(sourceDir, destFile, password)
    }

    private fun compressToZipEncrypted(sourceDir: File, destFile: File, password: String) {
        val zipOut = java.util.zip.ZipOutputStream(FileOutputStream(destFile))
        zipOut.setLevel(9)
        walkAndZipStored(zipOut, sourceDir, "")
        zipOut.close()

        val iv = ByteArray(16)
        java.security.SecureRandom().nextBytes(iv)
        val cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding")
        val keyDigest = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        val keySpec = javax.crypto.spec.SecretKeySpec(keyDigest, "AES")
        val ivSpec = javax.crypto.spec.IvParameterSpec(iv)
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, keySpec, ivSpec)

        val encFile = File(dataDir, "temp_enc_${UUID.randomUUID().toString().substring(0, 8)}")
        val destBytes = java.io.ByteArrayOutputStream()
        FileInputStream(destFile).use { inS -> inS.copyTo(destBytes) }
        val encryptedBytes = cipher.doFinal(destBytes.toByteArray())

        FileOutputStream(encFile).use { outStream ->
            DataOutputStream(outStream).use { allOut ->
                allOut.write(iv, 0, iv.size)
                allOut.write(encryptedBytes)
            }
        }
        destFile.delete()
        encFile.renameTo(destFile)
    }

    private fun compressToZip(sourceDir: File, destFile: File, zipMode: String) {
        val level = when (zipMode) { "max" -> 9; "extreme" -> 9; else -> 6 }
        java.util.zip.ZipOutputStream(FileOutputStream(destFile)).use { zos ->
            zos.setLevel(level)
            walkAndZipStored(zos, sourceDir, "")
        }
    }

    private fun compressToZipStored(sourceDir: File, destFile: File) {
        java.util.zip.ZipOutputStream(FileOutputStream(destFile)).use { zos ->
            zos.setLevel(0)
            walkAndZipStored(zos, sourceDir, "")
        }
    }

    private fun walkAndZipStored(zos: java.util.zip.ZipOutputStream, dir: File, base: String) {
        val files = dir.listFiles() ?: return
        for (file in files) {
            val name = if (base.isEmpty()) file.name else "$base/${file.name}"
            if (file.isDirectory) {
                zos.putNextEntry(java.util.zip.ZipEntry("$name/"))
                zos.closeEntry()
                walkAndZipStored(zos, file, name)
            } else {
                zos.putNextEntry(java.util.zip.ZipEntry(name))
                FileInputStream(file).use { inS -> inS.copyTo(zos) }
                zos.closeEntry()
            }
        }
    }

    private fun extractZip(zipFile: File, destDir: File) {
        java.util.zip.ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val filePath = File(destDir, entry.name)
                if (entry.isDirectory) {
                    filePath.mkdirs()
                } else {
                    filePath.parentFile?.mkdirs()
                    FileOutputStream(filePath).use { out -> zis.copyTo(out) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun deleteDir(dir: File) {
        if (!dir.exists()) return
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) deleteDir(file)
            else file.delete()
        }
        dir.delete()
    }

    private fun handleChooseDir(session: IHTTPSession): Response {
        val future = CompletableFuture<String>()
        dirPickerFuture.set(future)
        val filename = try {
            val body = readJsonBody(session)
            body.optString("filename", "prompt").trim().ifEmpty { "prompt" }
        } catch (_: Exception) { "prompt" }
        triggerDirPicker("$filename.bundle")
        return jsonResp(200, "{\"status\":\"pending\"}")
    }

    private fun handlePollChooseDir(session: IHTTPSession): Response {
        val future = dirPickerFuture.get() ?: return jsonResp(200, "{\"status\":\"pending\"}")
        return if (future.isDone) {
            dirPickerFuture.set(null)
            val uri = future.getNow("")
            if (uri.isBlank()) {
                jsonResp(200, "{\"cancelled\":true}")
            } else {
                selectedPackDirUri.set(uri)
                jsonResp(200, "{\"status\":\"done\",\"path\":\"${escapeJson(uri)}\"}")
            }
        } else {
            jsonResp(200, "{\"status\":\"pending\"}")
        }
    }

    private fun handleSelectLoadFile(session: IHTTPSession): Response {
        val seq = filePickerSeq.incrementAndGet()
        val future = CompletableFuture<FilePickerResult>()
        filePickerFuture.set(future)
        Log.i(TAG, "select_load_file_launch seq=$seq")
        triggerFilePicker()
        return jsonResp(200, "{\"status\":\"pending\"}")
    }

    private fun handlePollSelectLoadFile(session: IHTTPSession): Response {
        val future = filePickerFuture.get() ?: return jsonResp(200, "{\"status\":\"pending\"}")
        return if (future.isDone) {
            filePickerFuture.set(null)
            val result = future.getNow(FilePickerResult())
            if (!result.success && result.error == null) {
                jsonResp(200, "{\"cancelled\":true}")
            } else if (result.error != null) {
                val err = result.error ?: ""
                jsonResp(200, "{\"error\":\"${escapeJson(err)}\"}")
            } else {
                val filePath = result.path ?: ""
                selectedLoadFilePath.set(filePath)

                val fileBytes = try {
                    val file = File(filePath)
                    if (file.length() < 4) byteArrayOf()
                    else {
                        val head = ByteArray(4)
                        FileInputStream(file).use { fis ->
                            val need = 4
                            var offset = 0
                            while (offset < need) {
                                val n = fis.read(head, offset, need - offset)
                                if (n == -1) break
                                offset += n
                            }
                        }
                        head
                    }
                } catch (e: Exception) {
                    val msg = e.message ?: ""
                    return jsonResp(200, "{\"error\":\"${escapeJson(msg)}\"}")
                }

                if (fileBytes.size >= 4) {
                    val isEncrypted = !(fileBytes[0].toInt() == 0x50 && fileBytes[1].toInt() == 0x4B)
                    if (isEncrypted) {
                        return jsonResp(200, "{\"valid\":false,\"needPassword\":true,\"path\":\"${escapeJson(filePath)}\"}")
                    }
                }

                jsonResp(200, "{\"valid\":true,\"path\":\"${escapeJson(filePath)}\",\"filename\":\"${escapeJson(File(filePath).name)}\"}")
            }
        } else {
            jsonResp(200, "{\"status\":\"pending\"}")
        }
    }

    private fun handleUnlockLoadFile(session: IHTTPSession): Response {
        return try {
            val body = readJsonBody(session)
            val password = body.optString("password", "")
            val filePath = selectedLoadFilePath.get()
            if (filePath.isEmpty()) {
                return jsonResp(400, "{\"error\":\"no_file_selected\"}")
            }

            val sourceFile = File(filePath)
            val sourceLen = sourceFile.length()
            if (sourceLen < 16) {
                return jsonResp(200, "{\"valid\":false,\"error\":\"file_corrupted\"}")
            }

            val head = ByteArray(4)
            FileInputStream(sourceFile).use { fis ->
                var offset = 0
                while (offset < 4) {
                    val n = fis.read(head, offset, 4 - offset)
                    if (n == -1) break
                    offset += n
                }
            }
            val isEncrypted = !(sourceLen >= 4 && head[0].toInt() == 0x50 && head[1].toInt() == 0x4B)
            if (!isEncrypted) {
                return jsonResp(200, "{\"valid\":true}")
            }

            val tempFile = File(dataDir, "temp_decoded_${UUID.randomUUID().toString().substring(0, 8)}")
            try {
                FileInputStream(sourceFile).use { fis ->
                    val iv = ByteArray(16)
                    var ivOffset = 0
                    while (ivOffset < 16) {
                        val n = fis.read(iv, ivOffset, 16 - ivOffset)
                        if (n == -1) break
                        ivOffset += n
                    }
                    if (ivOffset < 16) {
                        tempFile.delete()
                        return jsonResp(200, "{\"valid\":false,\"error\":\"file_corrupted\"}")
                    }

                    val cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding")
                    val keyDigest = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
                    val keySpec = javax.crypto.spec.SecretKeySpec(keyDigest, "AES")
                    val ivSpec = javax.crypto.spec.IvParameterSpec(iv)
                    cipher.init(javax.crypto.Cipher.DECRYPT_MODE, keySpec, ivSpec)

                    FileOutputStream(tempFile).use { fos ->
                        val buf = ByteArray(8192)
                        var remaining = true
                        while (remaining) {
                            val bytesRead = fis.read(buf, 0, buf.size)
                            if (bytesRead > 0) {
                                val out = cipher.update(buf, 0, bytesRead)
                                if (out != null && out.size > 0) {
                                    fos.write(out, 0, out.size)
                                }
                            } else {
                                remaining = false
                            }
                        }
                        val finalBytes = cipher.doFinal()
                        if (finalBytes.size > 0) {
                            fos.write(finalBytes, 0, finalBytes.size)
                        }
                    }
                }

                FileInputStream(tempFile).use { fis ->
                    val check = ByteArray(2)
                    val n = fis.read(check, 0, 2)
                    if (n < 2 || check[0].toInt() != 0x50 || check[1].toInt() != 0x4B) {
                        tempFile.delete()
                        return jsonResp(200, "{\"valid\":false,\"needPassword\":true,\"error\":\"wrong_password\"}")
                    }
                }

                selectedLoadFilePath.set(tempFile.absolutePath)
                selectedLoadPathDecrypted.set(tempFile.absolutePath)
                jsonResp(200, "{\"valid\":true}")
            } catch (e: javax.crypto.BadPaddingException) {
                if (tempFile.exists()) tempFile.delete()
                jsonResp(200, "{\"valid\":false,\"needPassword\":true,\"error\":\"wrong_password\"}")
            } catch (e: Exception) {
                if (tempFile.exists()) tempFile.delete()
                val msg = e.message ?: "error"
                jsonResp(200, "{\"valid\":false,\"error\":\"$msg\"}")
            }
        } catch (e: Exception) {
            val msg = e.message ?: "error"
            jsonResp(200, "{\"valid\":false,\"error\":\"$msg\"}")
        }
    }

    private fun handleApplyLoad(session: IHTTPSession): Response {
        return try {
            val body = readJsonBody(session)
            val mode = body.optString("mode", "merge")
            Log.i(TAG, "handleApplyLoad_enter mode=$mode")
            // 立即快照当前路径，避免后台线程清除 shared state 时覆盖后续用户选择
            val decryptedPath = selectedLoadPathDecrypted.get()
            val path = if (decryptedPath.isNotEmpty() && File(decryptedPath).exists()) decryptedPath else selectedLoadFilePath.get()
            if (path.isNotEmpty()) {
                // 快照后立即清空共享状态，防止下一次选择/解锁写入被本线程的清理逻辑覆盖
                selectedLoadFilePath.set("")
                selectedLoadPathDecrypted.set("")
            }
            if (path.isEmpty()) {
                jsonResp(400, "{\"error\":\"no_file_selected\"}")
            } else {
                val taskId = UUID.randomUUID().toString()
                loadResults[taskId] = JSONObject().put("status", "pending").put("text", "正在加载提示词包...")
                Log.i(TAG, "handleApplyLoad task=$taskId path=$path mode=$mode")
                Thread {
                    try {
                        Log.i(TAG, "load_thread_start task=$taskId path=$path")
                        val result = promptManager.loadBundle(path, mode,
                            object : PromptManager.ProgressCallback {
                                override fun onProgress(text: String) {
                                    loadResults[taskId] = JSONObject().put("status", "pending").put("text", text)
                                }
                            })
                        // 若加载的是解密临时文件，清理掉
                        if (decryptedPath.isNotEmpty()) {
                            val temp = File(decryptedPath)
                            if (temp.exists()) temp.delete()
                        }
                        loadResults[taskId] = JSONObject()
                            .put("done", true).put("success", true)
                            .put("message", result.optString("message", "加载成功"))
                            .put("mode", result.optString("mode", mode))
                        Log.i(TAG, "load_thread_done task=$taskId success=true")
                    } catch (e: Exception) {
                        val msg = e.message ?: "load_failed"
                        loadResults[taskId] = JSONObject()
                            .put("done", true).put("success", false).put("error", msg)
                        Log.e(TAG, "load_bundle_failed task=$taskId", e)
                    }
                }.start()
                Log.i(TAG, "handleApplyLoad_exit task=$taskId response_sent")
                jsonResp(200, "{\"task_id\":\"$taskId\"}")
            }
        } catch (e: Exception) {
            val msg = e.message ?: "error"
            Log.e(TAG, "handleApplyLoad_error", e)
            jsonResp(500, "{\"error\":\"$msg\"}")
        }
    }

    private fun handleSaveImage(session: IHTTPSession): Response {
        return try {
            val body = readJsonBody(session)
            val filename = body.optString("filename")
            if (filename.isEmpty()) {
                return jsonResp(400, "{\"error\":\"missing_filename\"}")
            }

            val decodedName = try {
                java.net.URLDecoder.decode(filename, Charsets.UTF_8.name())
            } catch (e: Exception) { filename }

            val searchPaths = listOf(
                File(dataDir, "picture/$decodedName"),
                File(dataDir, "picture/$filename"),
                File(appContext.filesDir, "picture/$decodedName"),
                File(appContext.filesDir, "picture/$filename")
            )

            var srcFile: File? = null
            for (candidate in searchPaths) {
                if (candidate.exists()) { srcFile = candidate; break }
            }

            if (srcFile == null) {
                return jsonResp(404, "{\"error\":\"image_not_found\"}")
            }

            val mimeType = srcFile.name.substringAfterLast('.', "").lowercase().let {
                when (it) {
                    "png" -> "image/png"
                    "jpg", "jpeg" -> "image/jpeg"
                    "gif" -> "image/gif"
                    "webp" -> "image/webp"
                    else -> "image/png"
                }
            }

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, decodedName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/prompt_image")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val insertUri = activity.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
            )

            if (insertUri == null) {
                return jsonResp(500, "{\"error\":\"cannot_create_image\"}")
            }

            try {
                val outStream = activity.contentResolver.openOutputStream(insertUri)
                if (outStream == null) {
                    activity.contentResolver.delete(insertUri, null, null)
                    return jsonResp(500, "{\"error\":\"cannot_write_image\"}")
                }
                java.io.FileInputStream(srcFile).use { inS ->
                    outStream.use { out -> inS.copyTo(out) }
                }
            } finally {
                val completedValues = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                activity.contentResolver.update(insertUri, completedValues, null, null)
            }

            jsonResp(200, "{\"message\":\"image_saved\"}")
        } catch (e: Exception) {
            val msg = e.message ?: "error"
            jsonResp(500, "{\"error\":\"$msg\"}")
        }
    }

    private fun serveAsset(uri: String): Response {
        val assetName = when {
            uri == "/" || uri.isEmpty() -> "index.html"
            else -> uri.substring(1)
        }
        return try {
            val ext = assetName.substringAfterLast('.', "").lowercase()
            val mimeType = when (ext) {
                "html" -> "text/html; charset=utf-8"
                "css" -> "text/css; charset=utf-8"
                "js" -> "application/javascript; charset=utf-8"
                "json" -> "application/json; charset=utf-8"
                "png" -> "image/png"
                "jpg", "jpeg" -> "image/jpeg"
                "gif" -> "image/gif"
                "webp" -> "image/webp"
                "svg" -> "image/svg+xml"
                "ico" -> "image/x-icon"
                else -> "application/octet-stream"
            }
            val data = appContext.assets.open(assetName).readBytes()
            newFixedLengthResponse(Response.Status.OK, mimeType, data.inputStream(), data.size.toLong())
        } catch (e: Exception) {
            newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain",
                "Not found: $assetName")
        }
    }

    private fun jsonResp(status: Int, body: String): Response {
        val statusResp = when (status) {
            in 200..299 -> Response.Status.OK
            400 -> Response.Status.BAD_REQUEST
            404 -> Response.Status.NOT_FOUND
            else -> Response.Status.INTERNAL_ERROR
        }
        return newFixedLengthResponse(statusResp, "application/json; charset=utf-8", body)
    }

    private fun readJsonBody(session: IHTTPSession): JSONObject {
        val size = session.headers["content-length"]?.toIntOrNull() ?: 0
        if (size <= 0) return JSONObject()
        val bytes = readBytesFully(session.inputStream, size)
        val text = String(bytes, Charsets.UTF_8)
        return try { JSONObject(text) } catch (e: Exception) { JSONObject() }
    }

    private fun guessExtension(data: ByteArray): String {
        if (data.size >= 4) {
            val b0 = data[0].toInt() and 0xFF
            val b1 = data[1].toInt() and 0xFF
            val b2 = data[2].toInt() and 0xFF
            val b3 = data[3].toInt() and 0xFF

            if (b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && b3 == 0x47) return "png"
            if (b0 == 0xFF && b1 == 0xD8 && b2 == 0xFF) return "jpg"
            if (b0 == 0x47 && b1 == 0x49 && b2 == 0x46 && b3 == 0x38) return "gif"
            if (b0 == 0x52 && b1 == 0x49 && b2 == 0x46 && b3 == 0x46 && data.size >= 12) {
                val w0 = data[8].toInt() and 0xFF
                val w1 = data[9].toInt() and 0xFF
                val w2 = data[10].toInt() and 0xFF
                val w3 = data[11].toInt() and 0xFF
                if (w0 == 0x57 && w1 == 0x45 && w2 == 0x42 && w3 == 0x50) return "webp"
            }
        }
        return "dat"
    }

    private fun readBytesFully(inputStream: InputStream, expectedSize: Int): ByteArray {
        if (expectedSize <= 0) return ByteArray(0)
        val buffer = ByteArray(expectedSize)
        var offset = 0
        while (offset < expectedSize) {
            val read = inputStream.read(buffer, offset, expectedSize - offset)
            if (read <= 0) break
            offset += read
        }
        if (offset == expectedSize) return buffer
        return buffer.copyOfRange(0, offset)
    }

    private fun parseQuery(uri: String): Map<String, String> {
        val map = LinkedHashMap<String, String>()
        val idx = uri.indexOf('?')
        if (idx >= 0) {
            uri.substring(idx + 1).split('&').forEach { pair ->
                val kv = pair.split('=', limit = 2)
                if (kv.size == 2) map[kv[0]] = kv[1]
            }
        }
        return map
    }

    private fun sha1Hex(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-1")
        digest.update(data)
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun sha1HexStream(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-1")
        val buf = ByteArray(8192)
        var n = inputStream.read(buf)
        while (n > 0) {
            digest.update(buf, 0, n)
            n = inputStream.read(buf)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun guessExtensionStream(inputStream: InputStream): String {
        val buf = ByteArray(12)
        var offset = 0
        while (offset < 12) {
            val n = inputStream.read(buf, offset, 12 - offset)
            if (n == -1) break
            offset += n
        }
        if (offset >= 4) {
            val b0 = buf[0].toInt() and 0xFF
            val b1 = buf[1].toInt() and 0xFF
            val b2 = buf[2].toInt() and 0xFF
            val b3 = buf[3].toInt() and 0xFF
            if (b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && b3 == 0x47) return "png"
            if (b0 == 0xFF && b1 == 0xD8 && b2 == 0xFF) return "jpg"
            if (b0 == 0x47 && b1 == 0x49 && b2 == 0x46 && b3 == 0x38) return "gif"
            if (b0 == 0x52 && b1 == 0x49 && b2 == 0x46 && b3 == 0x46 && offset >= 12) {
                val w0 = buf[8].toInt() and 0xFF
                val w1 = buf[9].toInt() and 0xFF
                val w2 = buf[10].toInt() and 0xFF
                val w3 = buf[11].toInt() and 0xFF
                if (w0 == 0x57 && w1 == 0x45 && w2 == 0x42 && w3 == 0x50) return "webp"
            }
        }
        return "dat"
    }

    private fun copyFileStream(src: File, dest: File) {
        FileInputStream(src).use { inS ->
            FileOutputStream(dest).use { outS -> inS.copyTo(outS) }
        }
    }

    private fun fastJsonArr(list: java.util.ArrayList<String>): String {
        val sb = StringBuilder("[")
        for (i in list.indices) {
            if (i > 0) sb.append(",")
            sb.append("\"").append(escapeJson(list[i])).append("\"")
        }
        sb.append("]")
        return sb.toString()
    }

    private fun escapeJson(s: String): String {
        return s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    // ===== 应用信息 / 更新 =====

    /** 当前应用版本号（用于更新比对与缓存判断） */
    private fun appVersionName(): String {
        return try {
            val pm = appContext.packageManager
            val pkg = appContext.packageName
            val info = if (android.os.Build.VERSION.SDK_INT >= 33) {
                pm.getPackageInfo(pkg, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkg, 0)
            }
            info.versionName ?: "0"
        } catch (e: Exception) {
            "0"
        }
    }

    private fun versionTuple(v: String): List<Int> =
        v.replace('-', '.').replace('+', '.').split('.').map { it.toIntOrNull() ?: 0 }

    private fun versionGt(a: String, b: String): Boolean {
        val ta = versionTuple(a)
        val tb = versionTuple(b)
        val n = maxOf(ta.size, tb.size)
        for (i in 0 until n) {
            val x = ta.getOrElse(i) { 0 }
            val y = tb.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    /** 更新提示缓存文件：放在应用私有目录（filesDir），不依赖 WebView 的 localStorage */
    private val updateCacheFile: File
        get() = File(appContext.filesDir, "update_cache.json")

    private fun readUpdateCache(): JSONObject? {
        return try {
            val f = updateCacheFile
            if (f.exists()) JSONObject(f.readText(Charsets.UTF_8)) else null
        } catch (e: Exception) {
            null
        }
    }

    private fun writeUpdateCache(obj: JSONObject) {
        try {
            updateCacheFile.writeText(obj.toString(), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.w(TAG, "update_cache_write_failed: ${e.message ?: "error"}")
        }
    }

    private fun clearUpdateCache() {
        try {
            val f = updateCacheFile
            if (f.exists()) f.delete()
        } catch (e: Exception) { /* ignore */ }
    }

    private fun handleAppInfo(): Response {
        return try {
            val pm = appContext.packageManager
            val pkg = appContext.packageName
            val info = if (android.os.Build.VERSION.SDK_INT >= 33) {
                pm.getPackageInfo(pkg, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkg, 0)
            }
            val versionName = info.versionName ?: "0"
            val versionCode: Long = if (android.os.Build.VERSION.SDK_INT >= 28) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                val legacyCode = info.versionCode
                legacyCode.toLong()
            }
            jsonResp(200, "{\"version\":\"${escapeJson(versionName)}\",\"versionCode\":$versionCode}")
        } catch (e: Exception) {
            jsonResp(500, "{\"error\":\"${escapeJson(e.message ?: "app_info_failed")}\"}")
        }
    }

    /** 代理请求远程更新接口（WebView 直接请求会受跨域限制） */
    private fun handleUpdateCheck(): Response {
        return try {
            val conn = java.net.URL(updateApiUrl).openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.requestMethod = "GET"
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "PromptTool-Android")
            val code = conn.responseCode
            if (code !in 200..299) {
                conn.disconnect()
                return jsonResp(500, "{\"error\":\"http_$code\"}")
            }
            val text = conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
            conn.disconnect()
            val obj = JSONObject(text) // 先校验能解析

            // 缓存维护：有新版本且与缓存不同 → 记录（清掉旧的"当天不再显示"）
            //           没有新版本 → 删除上次的缓存文件
            try {
                val remoteVer = obj.optString("version", "")
                if (remoteVer.isNotEmpty() && versionGt(remoteVer, appVersionName())) {
                    val cache = readUpdateCache()
                    if (cache == null || cache.optString("version", "") != remoteVer) {
                        writeUpdateCache(JSONObject().apply {
                            put("version", remoteVer)
                            put("changelog", obj.optString("changelog", ""))
                            put("downloads", obj.optJSONObject("downloads") ?: JSONObject())
                            put("published_at", obj.optString("published_at", ""))
                            put("snooze_date", "")
                            put("cached_at", System.currentTimeMillis())
                        })
                    }
                } else {
                    clearUpdateCache()
                }
            } catch (e: Exception) {
                Log.w(TAG, "update_cache_maintain_failed: ${e.message ?: "error"}")
            }

            newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", text)
        } catch (e: Exception) {
            Log.w(TAG, "update_check_failed: ${e.message ?: "error"}")
            jsonResp(500, "{\"error\":\"${escapeJson(e.message ?: "check_failed")}\"}")
        }
    }

    /** 当天的日期字符串（yyyy-MM-dd，用于"当天不再显示"） */
    private fun todayString(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())

    /** 返回缓存的更新（检测到后每次启动都提示；勾了"当天不再显示"则当天不提示） */
    private fun handleUpdatePending(): Response {
        return try {
            val cache = readUpdateCache() ?: return jsonResp(200, "{\"show\":false}")
            val ver = cache.optString("version", "")
            if (ver.isEmpty() || !versionGt(ver, appVersionName())) {
                clearUpdateCache()
                return jsonResp(200, "{\"show\":false}")
            }
            // 当天已选择"不再显示" → 今天不弹，明天恢复
            if (cache.optString("snooze_date", "") == todayString()) {
                return jsonResp(200, "{\"show\":false}")
            }

            val info = JSONObject().apply {
                put("version", ver)
                put("changelog", cache.optString("changelog", ""))
                put("downloads", cache.optJSONObject("downloads") ?: JSONObject())
                put("published_at", cache.optString("published_at", ""))
            }
            jsonResp(200, JSONObject().apply {
                put("show", true)
                put("info", info)
            }.toString())
        } catch (e: Exception) {
            jsonResp(500, "{\"error\":\"${escapeJson(e.message ?: "pending_failed")}\"}")
        }
    }

    /** 记录"当天不再显示"（只影响今天） */
    private fun handleUpdateSnooze(): Response {
        return try {
            val cache = readUpdateCache()
            if (cache != null && cache.optString("version", "").isNotEmpty()) {
                cache.put("snooze_date", todayString())
                writeUpdateCache(cache)
            }
            jsonResp(200, "{\"ok\":true}")
        } catch (e: Exception) {
            jsonResp(500, "{\"error\":\"${escapeJson(e.message ?: "snooze_failed")}\"}")
        }
    }

    private fun handleUpdateDownload(session: IHTTPSession): Response {
        val body = readJsonBody(session)
        val url = body.optString("url", "").trim()
        if (url.isEmpty() || (!url.startsWith("https://") && !url.startsWith("http://"))) {
            return jsonResp(400, "{\"error\":\"invalid_url\"}")
        }
        if (updateDownloading) {
            return jsonResp(409, "{\"error\":\"already_downloading\"}")
        }
        updateDownloading = true
        updateDone = false
        updateSuccess = false
        updateError = null
        updateDownloaded = 0L
        updateTotal = 0L
        updateSpeed = 0.0
        updateApkPath = ""
        Thread { downloadUpdateApk(url) }.start()
        return jsonResp(200, "{\"started\":true}")
    }

    private fun downloadUpdateApk(url: String) {
        val dir = File(activity.cacheDir, "update")
        dir.mkdirs()
        val apk = File(dir, "prompttool-update.apk")
        val tmp = File(dir, "prompttool-update.apk.tmp")
        try {
            val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "PromptTool-Android")
            val code = conn.responseCode
            if (code !in 200..299) throw RuntimeException("HTTP $code")
            val total = conn.contentLengthLong
            updateTotal = if (total > 0) total else 0L

            conn.inputStream.use { input ->
                FileOutputStream(tmp).use { out ->
                    val buf = ByteArray(64 * 1024)
                    var downloaded = 0L
                    var lastTick = System.currentTimeMillis()
                    var lastBytes = 0L
                    while (true) {
                        val read = input.read(buf)
                        if (read <= 0) break
                        out.write(buf, 0, read)
                        downloaded += read
                        updateDownloaded = downloaded
                        val now = System.currentTimeMillis()
                        if (now - lastTick >= 500) {
                            updateSpeed = (downloaded - lastBytes) * 1000.0 / (now - lastTick)
                            lastBytes = downloaded
                            lastTick = now
                        }
                    }
                    out.flush()
                }
            }
            updateSpeed = 0.0
            if (tmp.length() <= 0L) throw RuntimeException("empty_file")
            if (apk.exists()) apk.delete()
            if (!tmp.renameTo(apk)) {
                tmp.copyTo(apk, overwrite = true)
                tmp.delete()
            }
            updateDownloaded = apk.length()
            if (updateTotal <= 0L) updateTotal = updateDownloaded
            updateApkPath = apk.absolutePath
            updateDone = true
            updateSuccess = true
            Log.i(TAG, "update_download_done: ${apk.absolutePath} size=${apk.length()}")
        } catch (e: Exception) {
            updateError = e.message ?: "download_failed"
            updateDone = true
            updateSuccess = false
            Log.e(TAG, "update_download_failed", e)
        } finally {
            updateDownloading = false
        }
    }

    private fun handleUpdateProgress(): Response {
        val obj = JSONObject()
            .put("downloading", updateDownloading)
            .put("done", updateDone)
            .put("success", updateSuccess)
            .put("downloaded", updateDownloaded)
            .put("total", updateTotal)
            .put("speed", updateSpeed)
        updateError?.let { obj.put("error", it) }
        return jsonResp(200, obj.toString())
    }

    private fun handleUpdateInstall(): Response {
        val path = updateApkPath
        if (path.isEmpty() || !File(path).exists()) {
            return jsonResp(400, "{\"error\":\"apk_not_found\"}")
        }
        return try {
            activity.runOnUiThread { installApk(File(path)) }
            jsonResp(200, "{\"installing\":true}")
        } catch (e: Exception) {
            jsonResp(500, "{\"error\":\"${escapeJson(e.message ?: "install_failed")}\"}")
        }
    }

    private fun installApk(apkFile: File) {
        try {
            // Android 8.0+ 需要「安装未知应用」权限
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
                !activity.packageManager.canRequestPackageInstalls()) {
                // 记录待安装文件：用户授权后返回应用时 MainActivity.onResume 会自动继续安装
                activity.pendingInstallApk = apkFile
                val settingsIntent = Intent(
                    android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    android.net.Uri.parse("package:${activity.packageName}")
                )
                settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                activity.startActivity(settingsIntent)
                Log.i(TAG, "request install permission")
                return
            }
            activity.pendingInstallApk = null
            activity.launchInstaller(apkFile)
        } catch (e: Exception) {
            Log.e(TAG, "install_failed", e)
        }
    }

    companion object {
        private const val PACK_DIR_NAME = "prompt_pack"
    }
}
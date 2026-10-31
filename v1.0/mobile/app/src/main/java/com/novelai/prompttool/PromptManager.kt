package com.novelai.prompttool

import android.content.Context
import android.util.Log
import org.json.*
import java.io.*
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private const val TAG = "PromptManager"
private const val PROMPT_FILE_NAME = "prompt.json"
private const val BACKUP_FILE_NAME = "prompt_backup.json"
private const val TEMP_DIR_NAME = "temp"
private const val PIC_DIR_NAME = "picture"
private const val PACK_DIR_NAME = "prompt_pack"

/**
 * 数据管理器：负责 prompt.json 的读写、图片管理、打包等操作。
 */
class PromptManager(private val context: Context) {

    interface ProgressCallback {
        fun onProgress(text: String)
    }

    private val filesDir: File = context.getExternalFilesDir(null) ?: context.filesDir
    private val promptFile: File = File(filesDir, PROMPT_FILE_NAME)
    private val backupFile: File = File(filesDir, BACKUP_FILE_NAME)
    private val tempDir: File = File(filesDir, TEMP_DIR_NAME)
    private val picDir: File = File(filesDir, PIC_DIR_NAME)
    private val packDir: File = File(filesDir, PACK_DIR_NAME)

    init {
        picDir.mkdirs()
        tempDir.mkdirs()
        packDir.mkdirs()
    }

    // ===== prompt.json 读写 =====

    fun loadPrompts(): String {
        if (!promptFile.exists()) return "{}"
        return try {
            promptFile.readText(Charsets.UTF_8)
        } catch (e: Exception) {
            "{}"
        }
    }

    fun savePrompts(jsonText: String) {
        try {
            JSONObject(jsonText)
            File(filesDir, PROMPT_FILE_NAME).writeText(jsonText, Charsets.UTF_8)
            File(filesDir, BACKUP_FILE_NAME).writeText(jsonText, Charsets.UTF_8)
        } catch (e: JSONException) {
            Log.e(TAG, "保存提示词失败，JSON 非法", e)
            throw RuntimeException("JSON 数据非法")
        }
    }

    // 流式保存（大文件避免内存溢出）—— 手动序列化，不构造完整字符串
    fun savePrompts(jsonObj: JSONObject) {
        try {
            val tmpFile = File(filesDir, PROMPT_FILE_NAME + ".tmp")
            val finalFile = File(filesDir, PROMPT_FILE_NAME)
            val backupFile = File(filesDir, BACKUP_FILE_NAME)
            val backupTmp = File(filesDir, BACKUP_FILE_NAME + ".tmp")

            // 写主文件
            writeObjectToFile(jsonObj, tmpFile)
            if (finalFile.exists()) finalFile.delete()
            tmpFile.renameTo(finalFile)

            // 写备份文件
            writeObjectToFile(jsonObj, backupTmp)
            if (backupFile.exists()) backupFile.delete()
            backupTmp.renameTo(backupFile)

            Log.i(TAG, "保存提示词完成（流式），大小=${finalFile.length()}")
        } catch (e: Exception) {
            Log.e(TAG, "保存提示词失败（流式）", e)
            throw RuntimeException("保存提示词失败：${e.message}")
        }
    }

    // ===== 添加提示词 =====

    fun addPrompt(group: String, item: JSONObject) {
        val data = loadPromptsMap()
        val items = data.optJSONArray(group) ?: JSONArray().apply { data.put(group, this) }
        for (i in 0 until items.length()) {
            val existing = items.getJSONObject(i)
            if (existing.optString("role") == item.optString("role") &&
                existing.optString("common") == item.optString("common")) {
                throw RuntimeException("提示词已存在")
            }
        }
        items.put(item)
        savePrompts(data.toString(2))
    }

    // ===== 更新提示词 =====

    fun updatePrompt(
        originalGroup: String, originalRole: String, originalCommon: String,
        newGroup: String, newItem: JSONObject
    ) {
        val data = loadPromptsMap()
        val originalItems = data.optJSONArray(originalGroup) ?: throw RuntimeException("原始分组不存在")
        val originalIdx = findItemIndex(originalItems, originalRole, originalCommon)
        if (originalIdx < 0) throw RuntimeException("未找到原始提示词")

        val originalItem = originalItems.getJSONObject(originalIdx)
        originalItems.remove(originalIdx)

        val newItems = data.optJSONArray(newGroup) ?: JSONArray().apply { data.put(newGroup, this) }
        for (i in 0 until newItems.length()) {
            val existing = newItems.getJSONObject(i)
            if (existing.optString("role") == newItem.optString("role") &&
                existing.optString("common") == newItem.optString("common")) {
                if (originalItems.length() == 0) data.remove(originalGroup)
                originalItems.put(originalItem)
                savePrompts(data.toString(2))
                throw RuntimeException("更新后与同组其他提示词冲突")
            }
        }

        newItems.put(newItem)
        if (originalItems.length() == 0) data.remove(originalGroup)
        savePrompts(data.toString(2))
    }

    // ===== 删除提示词（通过 ID） =====

    fun deletePromptById(id: String) {
        val data = loadPromptsMap()
        val iterator = data.keys()
        var found = false
        while (iterator.hasNext()) {
            val group = iterator.next()
            val items = data.optJSONArray(group) ?: continue
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                if (item.optString("id") == id) {
                    items.remove(i)
                    found = true
                    break
                }
            }
            if (found) {
                if (items.length() == 0) data.remove(group)
                break
            }
        }
        if (!found) throw RuntimeException("未找到该提示词")
        savePrompts(data.toString(2))
    }

    // ===== 删除提示词 =====

    fun deletePrompt(group: String, role: String, common: String) {
        val data = loadPromptsMap()
        val items = data.optJSONArray(group) ?: throw RuntimeException("分组不存在")
        val idx = findItemIndex(items, role, common)
        if (idx < 0) throw RuntimeException("未找到该提示词")
        items.remove(idx)
        if (items.length() == 0) data.remove(group)
        savePrompts(data.toString(2))
    }

    // ===== 打包提示词 =====

    fun packPrompts(filename: String, selectedIds: List<*>?, customOutputPath: String? = null) {
        val data = loadPromptsMap()
        val selectedSet = if (selectedIds != null) selectedIds.map { it.toString() }.toSet() else null

        val newData = JSONObject()
        val imageSet = LinkedHashSet<String>()

        data.keys().forEach { group ->
            val items = data.optJSONArray(group) ?: return@forEach
            val keepItems = JSONArray()
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val id = item.optString("id")
                val shouldInclude = selectedSet == null || selectedSet.contains(id)
                if (shouldInclude) {
                    keepItems.put(item)
                    val cover = item.optJSONArray("cover")
                    if (cover != null) {
                        for (j in 0 until cover.length()) {
                            imageSet.add(cover.getString(j))
                        }
                    }
                }
            }
            if (keepItems.length() > 0) {
                newData.put(group, keepItems)
            }
        }

        if (newData.length() == 0) throw RuntimeException("没有可打包的提示词")

        val tempPackDir = File(tempDir, "pack_tmp")
        deleteRecursively(tempPackDir)
        tempPackDir.mkdirs()
        val tempPicDir = File(tempPackDir, PIC_DIR_NAME)
        tempPicDir.mkdirs()

        File(tempPackDir, PROMPT_FILE_NAME).writeText(newData.toString(2), Charsets.UTF_8)

        imageSet.forEach { img ->
            val src = File(picDir, img)
            if (src.exists()) {
                copyFile(src, File(tempPicDir, img))
            }
        }

        val destFile = if (customOutputPath != null && customOutputPath.isNotBlank()) {
            val dir = File(customOutputPath)
            dir.mkdirs()
            File(dir, filename + ".bundle")
        } else {
            File(context.getExternalFilesDir(null), PACK_DIR_NAME + "/" + filename + ".bundle")
        }
        destFile.parentFile?.mkdirs()

        ZipOutputStream(FileOutputStream(destFile)).use { zos ->
            walkAndZip(zos, tempPackDir, "")
            if (tempPicDir.listFiles()?.isEmpty() == true) {
                zos.putNextEntry(ZipEntry("$PIC_DIR_NAME/"))
                zos.closeEntry()
            }
        }

        deleteRecursively(tempPackDir)
        Log.i(TAG, "打包完成: ${destFile.absolutePath}")
    }

    // ===== 加载提示词 =====

    fun loadBundle(zipPath: String, mode: String = "merge", callback: ProgressCallback? = null): JSONObject {
        callback?.onProgress("正在解压文件...")
        val cacheDir = File(tempDir, "load_tmp_${System.currentTimeMillis()}")
        cacheDir.mkdirs()

        ZipInputStream(FileInputStream(File(zipPath))).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val entryPath = entry.name
                if (entry.isDirectory) {
                    File(cacheDir, entryPath).mkdirs()
                } else {
                    val dest = File(cacheDir, entryPath)
                    dest.parentFile?.mkdirs()
                    FileOutputStream(dest).use { out ->
                        zis.copyTo(out)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val newPromptPath = File(cacheDir, PROMPT_FILE_NAME)
        if (!newPromptPath.exists()) throw RuntimeException("包中缺少 prompt.json")

        callback?.onProgress("正在解析提示词数据...")
        val newData = JSONObject(newPromptPath.readText(Charsets.UTF_8))
        val srcPicDir = File(cacheDir, PIC_DIR_NAME)

        if (mode == "replace") {
            callback?.onProgress("正在替换提示词和图片...")
            picDir.listFiles()?.forEach { it.delete() }
            if (srcPicDir.exists()) {
                srcPicDir.listFiles()?.forEach { src ->
                    copyFile(src, File(picDir, src.name))
                }
            }
            // 流式写入，避免 toString() 构造完整字符串导致 OOM
            writeObjectToFile(newData, promptFile)
            writeObjectToFile(newData, backupFile)
        } else {
            callback?.onProgress("正在合并提示词...")
            if (srcPicDir.exists()) {
                srcPicDir.listFiles()?.forEach { src ->
                    val dst = File(picDir, src.name)
                    if (!dst.exists()) copyFile(src, dst)
                }
            }
            // 流式合并写入临时文件，避免内存中全量拷贝
            val mergedFile = File(filesDir, PROMPT_FILE_NAME + ".merge.tmp")
            mergePromptsToFile(loadPromptsMap(), newData, mergedFile)
            if (promptFile.exists()) promptFile.delete()
            mergedFile.renameTo(promptFile)
            copyFile(promptFile, backupFile)
        }

        callback?.onProgress("正在保存数据...")
        deleteRecursively(cacheDir)
        return JSONObject("{\"message\":\"加载成功\",\"mode\":\"$mode\"}")
    }

    // ===== 流式 JSON 写入（手动序列化，避免大文件 OOM）=====

    private fun writeObjectToFile(jsonObj: JSONObject, file: File) {
        file.parentFile?.mkdirs()
        BufferedWriter(OutputStreamWriter(FileOutputStream(file), Charsets.UTF_8), 8192).use { writer ->
            writeObject(jsonObj, writer)
            writer.flush()
        }
    }

    private fun writeObject(obj: JSONObject, writer: Writer) {
        writer.write("{")
        val keys = obj.keys()
        var first = true
        while (keys.hasNext()) {
            if (!first) writer.write(",")
            first = false
            val key = keys.next()
            writer.write("\"")
            writer.write(escapeJson(key))
            writer.write("\":")
            val value = obj.opt(key)
            writeValue(value, writer)
        }
        writer.write("}")
    }

    private fun writeArray(arr: JSONArray, writer: Writer) {
        writer.write("[")
        for (i in 0 until arr.length()) {
            if (i > 0) writer.write(",")
            writeValue(arr.get(i), writer)
        }
        writer.write("]")
    }

    private fun writeValue(value: Any?, writer: Writer) {
        when (value) {
            null, JSONObject.NULL -> writer.write("null")
            is JSONObject -> writeObject(value, writer)
            is JSONArray -> writeArray(value, writer)
            is String -> { writer.write("\""); writer.write(escapeJson(value)); writer.write("\"") }
            is Number -> writer.write(value.toString())
            is Boolean -> writer.write(value.toString())
            else -> { writer.write("\""); writer.write(escapeJson(value.toString())); writer.write("\"") }
        }
    }

    // ===== 流式合并（直接写入文件，避免内存全量拷贝）=====

    private fun mergePromptsToFile(oldData: JSONObject, newData: JSONObject, outputFile: File) {
        val writtenGroups = mutableSetOf<String>()

        BufferedWriter(OutputStreamWriter(FileOutputStream(outputFile), Charsets.UTF_8), 8192).use { writer ->
            writer.write("{")
            var firstGroup = true

            // 旧数据中的所有分组
            val oldKeys = oldData.keys()
            while (oldKeys.hasNext()) {
                val group = oldKeys.next()
                if (!firstGroup) writer.write(",")
                firstGroup = false
                writtenGroups.add(group)

                writer.write("\"")
                writer.write(escapeJson(group))
                writer.write("\":[")
                val oldItems = oldData.getJSONArray(group)
                val newItems = newData.optJSONArray(group)

                for (i in 0 until oldItems.length()) {
                    if (i > 0) writer.write(",")
                    writeValue(oldItems.get(i), writer)
                }

                if (newItems != null) {
                    for (i in 0 until newItems.length()) {
                        val newItem = newItems.getJSONObject(i)
                        var dup = false
                        for (j in 0 until oldItems.length()) {
                            val existing = oldItems.getJSONObject(j)
                            if (existing.optString("role") == newItem.optString("role") &&
                                existing.optString("common") == newItem.optString("common")) {
                                dup = true
                                break
                            }
                        }
                        if (!dup) {
                            writer.write(",")
                            writeValue(newItem, writer)
                        }
                    }
                }
                writer.write("]")
            }

            // 只存在于新数据中的分组
            val newKeys = newData.keys()
            while (newKeys.hasNext()) {
                val group = newKeys.next()
                if (writtenGroups.contains(group)) continue
                if (!firstGroup) writer.write(",")
                firstGroup = false

                writer.write("\"")
                writer.write(escapeJson(group))
                writer.write("\":")
                writeValue(newData.getJSONArray(group), writer)
            }

            writer.write("}")
            writer.flush()
        }
    }

    private fun escapeJson(s: String): String {
        return s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    // ===== 工具函数 =====

    private fun loadPromptsMap(): JSONObject {
        return try {
            JSONObject(loadPrompts())
        } catch (e: Exception) {
            JSONObject()
        }
    }

    private fun findItemIndex(items: JSONArray, role: String, common: String): Int {
        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            if (item.optString("role") == role && item.optString("common") == common) {
                return i
            }
        }
        return -1
    }

    private fun copyFile(src: File, dest: File) {
        dest.parentFile?.mkdirs()
        FileInputStream(src).use { inputStream ->
            FileOutputStream(dest).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }

    private fun walkAndZip(zos: ZipOutputStream, dir: File, base: String) {
        dir.listFiles()?.forEach { file ->
            val name = if (base.isEmpty()) file.name else "$base/${file.name}"
            if (file.isDirectory) {
                walkAndZip(zos, file, name)
            } else {
                zos.putNextEntry(ZipEntry(name))
                FileInputStream(file).use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
    }

    private fun deleteRecursively(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursively(it) }
        }
        file.delete()
    }
}
